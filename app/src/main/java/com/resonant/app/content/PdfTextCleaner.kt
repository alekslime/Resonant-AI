package com.resonant.app.content

/**
 * Tidies the raw text of a PDF, one string per page, so it reads well aloud: page numbers and
 * running headers or footers are dropped, words split by a line-end hyphen are joined, ligatures
 * are spelled out. Pages stay separate paragraphs. Pure Kotlin, tested on the JVM.
 */
object PdfTextCleaner {

    private val HYPHEN_BREAK = Regex("""(\p{L})-\n(\p{Ll})""")
    private val PAGE_NUMBER = Regex("""^(?:page\s+)?\d{1,4}(?:\s*(?:of|/)\s*\d{1,4})?$""", RegexOption.IGNORE_CASE)
    private val LIGATURES = mapOf("\uFB00" to "ff", "\uFB01" to "fi", "\uFB02" to "fl", "\uFB03" to "ffi", "\uFB04" to "ffl")

    fun clean(pages: List<String>): String =
        cleanPages(pages).filter { it.isNotBlank() }.joinToString("\n\n")

    /** The same cleanup, one string per input page (blank for an empty page), so a page's number is kept. */
    fun cleanPages(pages: List<String>): List<String> {
        val lines = pages.map { page ->
            var p = page.replace("\r\n", "\n").replace('\r', '\n').replace("\u00AD", "")
            LIGATURES.forEach { (from, to) -> p = p.replace(from, to) }
            p.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        }
        val repeated = repeatedLines(lines)
        return lines
            .map { page -> page.filter { it !in repeated && !PAGE_NUMBER.matches(it) }.joinToString("\n") }
            .map { HYPHEN_BREAK.replace(it, "\$1\$2") }
    }

    /** Short lines on most pages (a header, a footer) when the document is long enough to tell. */
    private fun repeatedLines(pages: List<List<String>>): Set<String> {
        if (pages.size < 4) return emptySet()
        val needed = Math.ceil(pages.size * 0.6).toInt()
        return pages.flatMap { it.distinct() }
            .groupingBy { it }.eachCount()
            .filter { (line, count) -> count >= needed && line.length <= 100 }
            .keys
    }
}
