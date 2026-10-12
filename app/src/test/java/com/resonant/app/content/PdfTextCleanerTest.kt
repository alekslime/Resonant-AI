package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfTextCleanerTest {

    @Test
    fun pages_become_separate_paragraphs() {
        assertEquals("First page text.\n\nSecond page text.", PdfTextCleaner.clean(listOf("First page text.", "Second page text.")))
    }

    @Test
    fun a_word_split_by_a_line_end_hyphen_is_joined() {
        assertEquals("This is an example", PdfTextCleaner.clean(listOf("This is an exam-\nple")))
    }

    @Test
    fun a_real_hyphen_before_a_capital_is_kept() {
        assertEquals("Anglo-\nSaxon", PdfTextCleaner.clean(listOf("Anglo-\nSaxon")))
    }

    @Test
    fun page_numbers_are_dropped() {
        assertEquals("Body text here.", PdfTextCleaner.clean(listOf("Body text here.\n12")))
        assertEquals("Body text here.", PdfTextCleaner.clean(listOf("Page 3\nBody text here.")))
        assertEquals("Body text here.", PdfTextCleaner.clean(listOf("Body text here.\n3 of 10")))
    }

    @Test
    fun a_header_and_footer_repeated_on_most_pages_are_dropped() {
        val pages = (1..5).map { "ACME Annual Report\nUnique body $it.\nConfidential" }
        val cleaned = PdfTextCleaner.clean(pages)
        assertEquals(false, cleaned.contains("ACME"))
        assertEquals(false, cleaned.contains("Confidential"))
        assertEquals(true, cleaned.contains("Unique body 3."))
    }

    @Test
    fun a_repeated_line_is_kept_in_a_short_document() {
        val cleaned = PdfTextCleaner.clean(listOf("Same line\nA.", "Same line\nB."))
        assertEquals(true, cleaned.contains("Same line"))
    }

    @Test
    fun ligatures_and_soft_hyphens_are_cleaned() {
        assertEquals("The first office", PdfTextCleaner.clean(listOf("The \uFB01rst o\uFB03ce")))
        assertEquals("cooperation", PdfTextCleaner.clean(listOf("co\u00ADoperation")))
    }

    @Test
    fun empty_pages_are_dropped() {
        assertEquals("Only text.", PdfTextCleaner.clean(listOf("", "  \n ", "Only text.")))
        assertEquals("", PdfTextCleaner.clean(listOf("", "7")))
    }

    @Test
    fun clean_pages_keeps_one_entry_per_page_so_page_numbers_survive() {
        val pages = PdfTextCleaner.cleanPages(listOf("First page words.\n1", "7", "Third page words."))
        assertEquals(listOf("First page words.", "", "Third page words."), pages)
    }
}
