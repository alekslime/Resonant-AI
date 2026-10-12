package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfFiguresTest {

    @Test
    fun captions_are_found_in_any_case_and_spelling() {
        assertTrue(PdfFigures.hasCaption("Figure 3 shows the rise in sales."))
        assertTrue(PdfFigures.hasCaption("as in Fig. 2, the curve flattens"))
        assertTrue(PdfFigures.hasCaption("CHART 1: Revenue"))
        assertFalse(PdfFigures.hasCaption("The figure is not shown here."))
        assertFalse(PdfFigures.hasCaption("Plain text about cells."))
    }

    @Test
    fun a_page_with_a_big_picture_or_a_caption_is_looked_at() {
        assertTrue(PdfFigures.isCandidate("Some words.", bigImages = 1, scanned = false))
        assertTrue(PdfFigures.isCandidate("See Figure 4 below.", bigImages = 0, scanned = false))
        assertFalse(PdfFigures.isCandidate("Only words, no figures at all on this page of text.", bigImages = 0, scanned = false))
    }

    @Test
    fun a_sparse_scanned_page_is_looked_at_but_a_sparse_text_page_is_not() {
        assertTrue(PdfFigures.isCandidate("12", bigImages = 0, scanned = true))
        assertFalse(PdfFigures.isCandidate("12", bigImages = 0, scanned = false))
        assertFalse(PdfFigures.isCandidate("A full page of scanned words, long enough to be ordinary text.", bigImages = 0, scanned = true))
    }

    @Test
    fun only_the_first_pages_are_picked_in_order() {
        assertEquals(listOf(0, 2, 5), PdfFigures.pick(listOf(5, 0, 2)))
        assertEquals((0 until PdfFigures.MAX_FIGURE_PAGES).toList(), PdfFigures.pick((0 until 20).toList()))
    }

    @Test
    fun the_page_prompt_asks_for_charts_only_and_allows_none() {
        val prompt = PdfFigures.pagePrompt("Figure 1. Sales by year")
        assertTrue(prompt.contains("axis"))
        assertTrue(prompt.contains("NONE"))
        assertTrue(prompt.contains("do not repeat them"))
        assertTrue(prompt.endsWith("Figure 1. Sales by year"))
        assertFalse(PdfFigures.pagePrompt("").contains("words on this page"))
    }

    @Test
    fun none_is_recognised_in_its_usual_forms() {
        assertTrue(PdfFigures.isNone("NONE"))
        assertTrue(PdfFigures.isNone(" None. "))
        assertTrue(PdfFigures.isNone("None of these."))
        assertFalse(PdfFigures.isNone("A bar chart of sales by year, rising from two to five million dollars."))
    }

    @Test
    fun only_a_real_description_is_usable() {
        assertTrue(PdfFigures.usable("A line graph of temperature over a year, peaking in July at about thirty degrees."))
        assertFalse(PdfFigures.usable("NONE"))
        assertFalse(PdfFigures.usable("A chart."))
        assertFalse(PdfFigures.usable(null))
    }

    @Test
    fun an_explanation_goes_right_after_its_page_and_is_announced() {
        val text = PdfFigures.compose(
            listOf("Page one words.", "", "Page three words, see Figure 1."),
            mapOf(2 to "# A bar chart\n\n- **Bars** rise from two to five million dollars over four quarters.")
        )
        assertEquals(
            "Page one words.\n\nPage three words, see Figure 1.\n\nFigure on page 3.\n\n" +
                "A bar chart\n\nBars rise from two to five million dollars over four quarters.",
            text
        )
    }

    @Test
    fun without_explanations_the_text_is_just_the_pages() {
        assertEquals("One.\n\nThree.", PdfFigures.compose(listOf("One.", "  ", "Three."), emptyMap()))
    }

    @Test
    fun the_composed_text_becomes_a_lesson_with_the_figure_in_place() {
        val text = PdfFigures.compose(
            listOf("Sales grew steadily this year. See Figure 1."),
            mapOf(0 to "A bar chart of sales by quarter, rising from two million to five million dollars.")
        )
        val lesson = MaterialParser.parse("m1", "report", text)!!
        val spoken = lesson.sections.flatMap { it.units }.map { it.text }
        assertTrue(spoken.contains("Figure on page 1."))
        assertTrue(spoken.indexOf("Figure on page 1.") > spoken.indexOfFirst { it.startsWith("Sales grew") })
    }
}
