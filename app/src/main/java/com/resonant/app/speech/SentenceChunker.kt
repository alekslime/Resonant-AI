package com.resonant.app.speech

/**
 * Turns a stream of text deltas into whole, speakable sentences.
 *
 * A boundary is sentence-ending punctuation (or a line break) FOLLOWED BY
 * whitespace. Requiring the whitespace is what keeps "3.5", "U.S.A" and a
 * sentence whose final delta hasn't arrived yet from being cut early; the last
 * sentence, which has no trailing whitespace, is released by [flush].
 */
class SentenceChunker {

    private val buffer = StringBuilder()

    /** Feed the next delta; returns any sentences that are now complete (possibly none). */
    fun feed(delta: String): List<String> {
        buffer.append(delta)
        val out = mutableListOf<String>()
        while (true) {
            val end = findBoundary() ?: break
            val sentence = SpeechText.clean(buffer.substring(0, end))
            buffer.delete(0, end)
            if (sentence.isNotEmpty()) out += sentence
        }
        return out
    }

    /** Call when the stream ends: releases whatever text is left as a final sentence. */
    fun flush(): String? {
        val rest = SpeechText.clean(buffer.toString())
        buffer.clear()
        return rest.ifEmpty { null }
    }

    /** Index just past the first real sentence boundary in the buffer, or null. */
    private fun findBoundary(): Int? {
        var i = 0
        while (i < buffer.length) {
            val c = buffer[i]
            val isTerminator = c == '.' || c == '!' || c == '?'
            if (isTerminator || c == '\n') {
                // A closing quote or bracket belongs to the sentence it closes: `said "no." Then`.
                var end = i + 1
                if (isTerminator) {
                    while (end < buffer.length && buffer[end] in CLOSERS) end++
                }
                // Need to SEE the whitespace after it; if the buffer ends first, wait for more.
                if (end < buffer.length && (c == '\n' || buffer[end].isWhitespace())) {
                    if (!isFalseBoundary(buffer.substring(0, i + 1), c)) return end
                }
            }
            i++
        }
        return null
    }

    /** "1." (a list number) and "Dr." / "e.g." are not the end of a sentence. */
    private fun isFalseBoundary(candidate: String, terminator: Char): Boolean {
        if (terminator != '.') return false
        val trimmed = candidate.trim()
        if (LIST_NUMBER.matches(trimmed)) return true
        val lastWord = trimmed.dropLast(1).substringAfterLast(' ').substringAfterLast('\n').lowercase()
        return lastWord in ABBREVIATIONS
    }

    private companion object {
        const val CLOSERS = "\"')]\u201D\u2019"
        val LIST_NUMBER = Regex("""\d+\.""")
        val ABBREVIATIONS = setOf("mr", "mrs", "ms", "dr", "prof", "st", "vs", "etc", "e.g", "i.e")
    }
}

/** Cleanup for text that will be read aloud, not displayed. */
object SpeechText {

    private val MARKDOWN_SYMBOLS = Regex("[*_`#~]+")
    private val LEADING_BULLET = Regex("""^\s*[-•]\s+""")
    private val WHITESPACE = Regex("""\s+""")

    /** Strips markdown the model wasn't supposed to emit, and collapses whitespace. */
    fun clean(text: String): String =
        text.replace(MARKDOWN_SYMBOLS, "")
            .replace(LEADING_BULLET, "")
            .replace(WHITESPACE, " ")
            .trim()
}
