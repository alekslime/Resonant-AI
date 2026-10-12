package com.resonant.app.content

/**
 * The pure side of explaining graphs and figures inside a PDF: which pages probably have one,
 * what the vision model is asked about a page, and how its answer is put back after that page's
 * text. Pure Kotlin, tested on the JVM. The reading and rendering is in [PdfTextExtractor] and
 * [ImageTextReader]; the loop that calls the model is in [MaterialImporter].
 */
object PdfFigures {

    /** At most this many pages are sent to the vision model, because each can take a minute on a CPU. */
    const val MAX_FIGURE_PAGES = 6

    /** A scanned page with fewer letters than this is probably a picture, so it is looked at. */
    private const val SPARSE_SCAN_LETTERS = 40

    /** At most this much of a page's own text is shown to the model, as a hint for exact labels. */
    private const val HINT_CHARS = 1500

    private val CAPTION = Regex("""\b(?:fig\.?|figure|chart|graph|diagram|plot)\s*[0-9]+""", RegexOption.IGNORE_CASE)

    /** True when [text] has a caption such as "Figure 3", "Fig. 2" or "Chart 1". */
    fun hasCaption(text: String) = CAPTION.containsMatchIn(text)

    /**
     * Whether page text [text] is worth showing to the vision model. [bigImages] is how many large
     * pictures the page embeds (always 0 for a scan, where the whole page is one picture). Charts drawn
     * as lines have no embedded picture, so a caption counts too. A sparse scanned page counts as well.
     */
    fun isCandidate(text: String, bigImages: Int, scanned: Boolean): Boolean =
        bigImages > 0 || hasCaption(text) ||
            (scanned && text.count { it.isLetter() } < SPARSE_SCAN_LETTERS)

    /** The first [MAX_FIGURE_PAGES] of the candidate page numbers, in reading order. */
    fun pick(candidates: List<Int>): List<Int> = candidates.sorted().take(MAX_FIGURE_PAGES)

    /** What the vision model is asked about one page. [pageText] is the page's own words, as a hint. */
    fun pagePrompt(pageText: String): String {
        val hint = pageText.trim().take(HINT_CHARS)
        return buildString {
            append("This is one page of a document, and it will be read aloud to a listener who cannot see it. ")
            append("The words on the page are read separately, so do not repeat them. Describe only the charts, graphs, tables, ")
            append("diagrams and pictures on it, in plain sentences with no Markdown, no bullet points and no headings. ")
            append("For a chart or graph: say what kind it is and its title; what each axis shows and its units; ")
            append("what each line, bar, slice or series stands for; the overall trend; the highest and lowest values; ")
            append("and how the main groups compare. Give numbers when you can read them, and say 'about' when you are estimating. ")
            append("For a table, read it row by row. For a diagram, name the parts and say how they connect, in order. ")
            append("Never invent a number or a label you cannot read. ")
            append("If the page has no chart, graph, table, diagram or picture, answer with the single word NONE.")
            if (hint.isNotEmpty()) {
                append("\n\nThe words on this page, which may contain mistakes:\n")
                append(hint)
            }
        }
    }

    /** True when the model said the page has nothing to describe. */
    fun isNone(reply: String): Boolean {
        val t = reply.trim().trim('.', '!', '"', '\'', '*', ' ')
        return t.equals("none", ignoreCase = true) || (t.length < 40 && t.startsWith("none", ignoreCase = true))
    }

    /** True when [reply] is a real description and not "NONE" or a fragment. */
    fun usable(reply: String?): Boolean = reply != null && !isNone(reply) && ImageMaterial.hasDescription(reply)

    /**
     * The cleaned pages (one string each, blank where a page had no words) as one text, each page's
     * explanation placed right after it, announced as "Figure on page N." so the listener knows where it is.
     * [descriptions] is keyed by zero-based page index.
     */
    fun compose(pages: List<String>, descriptions: Map<Int, String>): String {
        val parts = mutableListOf<String>()
        pages.forEachIndexed { index, text ->
            if (text.isNotBlank()) parts += text.trim()
            descriptions[index]?.let {
                parts += "Figure on page ${index + 1}."
                parts += ImageMaterial.tidyDescription(it)
            }
        }
        return parts.joinToString("\n\n")
    }
}
