package com.resonant.app.content

/**
 * Turns the person's own text or Markdown into a [Lesson] the app can already read aloud:
 * headings become sections, sentences become spoken units. Markdown is cleaned for the ear
 * (links keep their words, URLs become "a link", code blocks are announced, not read out).
 * Pure Kotlin, tested on the JVM.
 */
object MaterialParser {

    /** No unit is longer than this, so pause and repeat stay useful. */
    const val MAX_UNIT_CHARS = 320

    /** A section longer than [MAX_SECTION_WORDS] is cut into parts of about this many words. */
    const val SECTION_TARGET_WORDS = 250
    const val MAX_SECTION_WORDS = 600

    const val CODE_NOTE = "There is a code block here."

    private val FRONT_MATTER = Regex("""\A---[ \t]*\n.*?\n---[ \t]*(\n|\z)""", RegexOption.DOT_MATCHES_ALL)
    private val HEADING = Regex("""^\s{0,3}#{1,6}\s+(.+?)\s*#*\s*$""")
    private val FENCE = Regex("""^\s*(```|~~~)""")
    private val RULE = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")
    private val TABLE_SEPARATOR = Regex("""^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$""")
    private val LIST_ITEM = Regex("""^\s*(?:[-*+•]|\d{1,3}[.)])\s+(.*)$""")
    private val QUOTE = Regex("""^\s{0,3}>+\s?""")
    private val IMAGE = Regex("""!\[([^\]]*)]\([^)]*\)""")
    private val LINK = Regex("""\[([^\]]+)]\([^)]*\)""")
    private val URL = Regex("""https?://\S+""")
    private val HTML_TAG = Regex("""</?[a-zA-Z][^>]*>""")
    private val EMPHASIS = Regex("""(?<!\w)(\*{1,3}|_{1,3})(\S(?:.*?\S)?)\1(?!\w)""")
    private val SNAKE_CASE = Regex("""(?<=[A-Za-z0-9])_(?=[A-Za-z0-9])""")
    private val SPACES = Regex("""\s+""")
    private val ABBREVIATIONS = setOf("mr", "mrs", "ms", "dr", "prof", "st", "vs", "etc", "e.g", "i.e", "fig")

    private class RawSection(val title: String?) {
        val paragraphs = mutableListOf<String>()
    }

    /** "my_notes-final.md" -> "my notes final". */
    fun titleFromFileName(name: String): String {
        val base = name.substringBeforeLast('.', name).replace(Regex("""[_\-]+"""), " ").replace(SPACES, " ").trim()
        return base.ifEmpty { "Untitled material" }
    }

    /** Null when the text has nothing readable in it. */
    fun parse(id: String, title: String, text: String): Lesson? {
        val raw = readSections(text)
        val hasHeadings = raw.any { it.title != null }

        val sections = mutableListOf<LessonSection>()
        for (section in raw) {
            val chunks = chunk(section.paragraphs)
            chunks.forEachIndexed { n, paragraphs ->
                val sectionTitle = when {
                    chunks.size == 1 -> section.title ?: title
                    section.title == null && !hasHeadings -> "Part ${n + 1}"
                    else -> "${section.title ?: title}, part ${n + 1}"
                }
                val sectionId = "${id}_s${sections.size + 1}"
                val units = paragraphs.flatMap { sentences(it) }
                    .mapIndexed { u, s -> SemanticUnit("${sectionId}u${u + 1}", s) }
                if (units.isNotEmpty()) sections += LessonSection(sectionId, sectionTitle, units)
            }
        }
        return if (sections.isEmpty()) null else Lesson(id, title, sections)
    }

    private fun readSections(text: String): List<RawSection> {
        val body = FRONT_MATTER.replaceFirst(
            text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n'), ""
        )
        val sections = mutableListOf<RawSection>()
        var current = RawSection(null)
        val paragraph = StringBuilder()
        var inFence = false

        fun endParagraph() {
            val p = cleanInline(paragraph.toString())
            paragraph.clear()
            if (p.any { it.isLetterOrDigit() }) current.paragraphs += p
        }

        for (line in body.split('\n')) {
            if (FENCE.containsMatchIn(line)) {
                endParagraph()
                if (!inFence) current.paragraphs += CODE_NOTE
                inFence = !inFence
                continue
            }
            if (inFence) continue
            val l = QUOTE.replaceFirst(line, "")
            if (l.isBlank()) { endParagraph(); continue }
            val heading = HEADING.matchEntire(l)
            if (heading != null) {
                endParagraph()
                sections += current
                current = RawSection(cleanInline(heading.groupValues[1]))
                continue
            }
            if (RULE.matches(l) || TABLE_SEPARATOR.matches(l)) { endParagraph(); continue }
            val item = LIST_ITEM.matchEntire(l)
            if (item != null) {
                endParagraph()
                paragraph.append(item.groupValues[1])
                endParagraph()
                continue
            }
            if (l.trim().startsWith("|")) {
                endParagraph()
                paragraph.append(l.trim().trim('|').split('|').map { cleanInline(it) }.filter { it.isNotEmpty() }.joinToString(", "))
                endParagraph()
                continue
            }
            paragraph.append(' ').append(l.trim())
        }
        endParagraph()
        sections += current
        return sections.filter { it.paragraphs.isNotEmpty() }
    }

    private fun cleanInline(raw: String): String {
        var t = raw
        t = IMAGE.replace(t) { it.groupValues[1].ifBlank { "image" } }
        t = LINK.replace(t) { it.groupValues[1] }
        t = URL.replace(t, "a link")
        t = HTML_TAG.replace(t, "")
        t = EMPHASIS.replace(t) { it.groupValues[2] }
        t = t.replace("`", "")
        t = SNAKE_CASE.replace(t, " ")
        return SPACES.replace(t, " ").trim()
    }

    /** Paragraphs grouped into parts of about [SECTION_TARGET_WORDS] words, only when the section is long. */
    private fun chunk(paragraphs: List<String>): List<List<String>> {
        fun words(p: String) = p.count { it == ' ' } + 1
        if (paragraphs.sumOf { words(it) } <= MAX_SECTION_WORDS) return listOf(paragraphs)
        val chunks = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        var count = 0
        for (p in paragraphs) {
            current += p
            count += words(p)
            if (count >= SECTION_TARGET_WORDS) {
                chunks += current
                current = mutableListOf()
                count = 0
            }
        }
        if (current.isNotEmpty()) {
            if (count < SECTION_TARGET_WORDS / 2 && chunks.isNotEmpty()) chunks.last().addAll(current) else chunks += current
        }
        return chunks
    }

    private fun sentences(paragraph: String): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < paragraph.length) {
            val c = paragraph[i]
            if (c == '.' || c == '!' || c == '?') {
                var end = i + 1
                while (end < paragraph.length && paragraph[end] in ".!?\"')]\u201D\u2019") end++
                if (end >= paragraph.length) break
                if (paragraph[end].isWhitespace()) {
                    var next = end
                    while (next < paragraph.length && paragraph[next].isWhitespace()) next++
                    if (next >= paragraph.length) break
                    val before = paragraph.substring(start, i)
                    val word = before.substringAfterLast(' ').lowercase()
                    val falseStop = paragraph[next].isLowerCase() ||
                        (c == '.' && (word in ABBREVIATIONS || (word.length == 1 && word[0].isLetter()) ||
                            (word.isNotEmpty() && word.all { it.isDigit() } && before.trim() == word)))
                    if (!falseStop) {
                        out += paragraph.substring(start, end).trim()
                        start = next
                        i = next
                        continue
                    }
                }
                i = end
                continue
            }
            i++
        }
        paragraph.substring(start).trim().takeIf { it.isNotEmpty() }?.let { out += it }
        return out.flatMap { limitLength(it) }
    }

    private fun limitLength(sentence: String): List<String> {
        val out = mutableListOf<String>()
        var rest = sentence
        while (rest.length > MAX_UNIT_CHARS) {
            val stop = maxOf(rest.lastIndexOf(", ", MAX_UNIT_CHARS - 1), rest.lastIndexOf("; ", MAX_UNIT_CHARS - 1), rest.lastIndexOf(": ", MAX_UNIT_CHARS - 1))
            val cut = when {
                stop >= MAX_UNIT_CHARS / 2 -> stop + 1
                else -> rest.lastIndexOf(' ', MAX_UNIT_CHARS).let { if (it > 0) it else MAX_UNIT_CHARS }
            }
            out += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) out += rest
        return out
    }
}
