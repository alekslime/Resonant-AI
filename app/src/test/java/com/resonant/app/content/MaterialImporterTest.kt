package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MaterialImporterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val store get() = MaterialStore(File(tmp.root, "materials"))
    private fun decode(name: String, text: String) = MaterialImporter.decode(store, name, text.toByteArray())

    @Test
    fun a_text_file_is_saved() {
        val r = decode("notes.txt", "Cells are small.") as MaterialImporter.Result.Saved
        assertEquals("notes", r.entry.title)
        assertEquals(1, store.list().size)
    }

    @Test
    fun markdown_extensions_are_accepted_in_any_case() {
        assertTrue(decode("a.md", "Hello.") is MaterialImporter.Result.Saved)
        assertTrue(decode("b.MD", "Hello.") is MaterialImporter.Result.Saved)
        assertTrue(decode("c.markdown", "Hello.") is MaterialImporter.Result.Saved)
    }

    @Test
    fun a_name_without_an_extension_is_treated_as_text() {
        assertTrue(decode("Shared text", "Hello.") is MaterialImporter.Result.Saved)
    }

    @Test
    fun other_file_types_are_refused() {
        assertEquals(MaterialImporter.Result.NotText, decode("photo.jpg", "Hello."))
        assertEquals(MaterialImporter.Result.NotText, decode("book.pdf", "Hello."))
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun binary_content_is_refused_even_with_a_text_name() {
        val r = MaterialImporter.decode(store, "x.txt", byteArrayOf(72, 0, 105))
        assertEquals(MaterialImporter.Result.NotText, r)
    }

    @Test
    fun a_file_over_the_limit_is_refused() {
        val r = MaterialImporter.decode(store, "big.txt", ByteArray(MaterialImporter.MAX_BYTES + 1) { 97 })
        assertEquals(MaterialImporter.Result.TooLarge, r)
    }

    @Test
    fun a_file_with_nothing_readable_is_empty() {
        assertEquals(MaterialImporter.Result.Empty, decode("blank.txt", "  \n \n"))
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun pdf_text_is_saved_under_the_file_name() {
        val text = "This report has plenty of readable words in it, enough to count as real text."
        val r = MaterialImporter.decodePdfText(store, "report.pdf", text) as MaterialImporter.Result.Saved
        assertEquals("report", r.entry.title)
        assertEquals(1, store.list().size)
    }

    @Test
    fun a_pdf_with_no_text_is_reported_as_a_scan_and_not_saved() {
        assertEquals(MaterialImporter.Result.PdfNoText, MaterialImporter.decodePdfText(store, "scan.pdf", "  \n 12 \n"))
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun a_pdf_is_recognised_by_name_or_by_content() {
        assertTrue(MaterialImporter.isPdf("Book.PDF", ByteArray(0)))
        assertTrue(MaterialImporter.isPdf("download", "%PDF-1.7 ...".toByteArray()))
        assertEquals(false, MaterialImporter.isPdf("notes.txt", "Hello".toByteArray()))
    }

    @Test
    fun every_result_has_a_spoken_message() {
        val saved = MaterialImporter.Result.Saved(MaterialStore.Entry("m1", "My notes", "My notes.txt", 0))
        assertEquals("Added My notes to your lessons.", MaterialImporter.message(saved))
        listOf(
            MaterialImporter.Result.Empty, MaterialImporter.Result.TooLarge,
            MaterialImporter.Result.NotText, MaterialImporter.Result.Failed,
            MaterialImporter.Result.PdfLocked, MaterialImporter.Result.PdfNoText
        ).forEach { assertTrue(MaterialImporter.message(it).isNotBlank()) }
    }
}
