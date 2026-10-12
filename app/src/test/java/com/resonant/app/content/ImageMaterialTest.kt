package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageMaterialTest {

    @Test
    fun images_are_recognised_by_name_or_content() {
        assertTrue(ImageMaterial.isImage("IMG_1.JPG", ByteArray(0)))
        assertTrue(ImageMaterial.isImage("shot.webp", ByteArray(0)))
        assertTrue(ImageMaterial.isImage("download", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())))
        assertTrue(ImageMaterial.isImage("download", byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())))
        assertTrue(ImageMaterial.isImage("download", "RIFF....WEBPVP8 ".toByteArray()))
        assertFalse(ImageMaterial.isImage("notes.txt", "Hello".toByteArray()))
        assertFalse(ImageMaterial.isImage("book.pdf", "%PDF-1.7".toByteArray()))
    }

    @Test
    fun sample_size_keeps_the_longest_side_at_or_above_the_target() {
        assertEquals(1, ImageMaterial.sampleSize(1000, 800))
        assertEquals(1, ImageMaterial.sampleSize(3000, 2000))
        assertEquals(2, ImageMaterial.sampleSize(4000, 3000))
        assertEquals(4, ImageMaterial.sampleSize(8000, 6000))
    }

    @Test
    fun fit_size_shrinks_but_never_enlarges() {
        assertEquals(800 to 600, ImageMaterial.fitSize(800, 600))
        val (w, h) = ImageMaterial.fitSize(3136, 2352)
        assertEquals(ImageMaterial.MAX_SIDE, w)
        assertEquals(1176, h)
        assertEquals(1176 to ImageMaterial.MAX_SIDE, ImageMaterial.fitSize(2352, 3136))
    }

    @Test
    fun recognised_blocks_become_paragraphs_with_wrapped_lines_joined() {
        val text = ImageMaterial.cleanText(listOf("The cell is the basic\nunit of life.", "  ", "Mito-\nchondria make energy."))
        assertEquals("The cell is the basic unit of life.\n\nMitochondria make energy.", text)
    }

    @Test
    fun a_description_and_its_words_become_two_sections() {
        val material = ImageMaterial.compose(
            "Revenue\n\nQ1 Q2 Q3 Q4",
            "A bar chart of revenue by quarter. It rises from about two million to five million."
        )!!
        assertTrue(material.startsWith("## What the picture shows"))
        assertTrue(material.contains("## Text in the picture"))
        val lesson = MaterialParser.parse("m1", "chart", material)!!
        assertEquals(listOf("What the picture shows", "Text in the picture"), lesson.sections.map { it.title })
    }

    @Test
    fun words_alone_are_kept_plain_and_noise_alone_is_dropped() {
        assertEquals("Photosynthesis turns light into sugar.", ImageMaterial.compose("Photosynthesis turns light into sugar.", null))
        assertNull(ImageMaterial.compose("l1 |", null))
        assertNull(ImageMaterial.compose("", "short"))
    }

    @Test
    fun a_description_alone_is_enough_for_a_picture_with_no_words() {
        val material = ImageMaterial.compose("", "A line graph with no labels, rising steadily from left to right.")!!
        assertTrue(material.startsWith("## What the picture shows"))
        assertFalse(material.contains("Text in the picture"))
    }

    @Test
    fun markdown_in_a_description_is_removed() {
        val tidy = ImageMaterial.tidyDescription("# Chart\n\n- **Bars** show sales\n- Lines show cost\n\n\n\nDone.")
        assertEquals("Chart\n\nBars show sales\nLines show cost\n\nDone.", tidy)
    }

    @Test
    fun the_prompt_asks_for_chart_details_and_carries_the_recognised_text() {
        val prompt = ImageMaterial.prompt("Year  Sales")
        assertTrue(prompt.contains("axis"))
        assertTrue(prompt.contains("trend"))
        assertTrue(prompt.endsWith("Year  Sales"))
        assertFalse(ImageMaterial.prompt("").contains("Text already found"))
        assertTrue(ImageMaterial.prompt("x".repeat(5000)).length < 5000)
    }
}
