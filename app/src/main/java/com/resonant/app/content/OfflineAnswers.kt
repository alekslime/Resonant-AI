package com.resonant.app.content

/**
 * What Chat says when [com.resonant.app.network.OllamaClient] can't be reached at all —
 * server off, wrong Wi-Fi, phone offline. A demo that just goes silent there is worse
 * than one that visibly falls back, so instead of only playing [ERROR][com.resonant.app.haptics.HapticPattern.ERROR]
 * and stopping, Chat still answers from the one lesson bundled in the app.
 *
 * This is not a model. It picks the [LessonSection] whose title, keywords, and spoken
 * text share the most words with the question, and reads that section back. It only
 * ever repeats existing lesson content — it does not compose new sentences — so a
 * fabricated-sounding "AI" answer never comes out of an offline path.
 */
object OfflineAnswers {

    /** Said once per offline reply, so nobody mistakes canned lesson text for a live model. */
    const val PREFACE = "I can't reach the AI server right now, so here's what the lesson says. "

    /** Read when no section scores above [MIN_SCORE] — points at what can still be asked. */
    private val fallbackText: String by lazy {
        val topics = LessonData.binarySearchLesson.sections.joinToString(", ") { it.title.lowercase() }
        "I can't reach the AI server right now, and I couldn't match that to the lesson. " +
            "Try asking about one of these: $topics."
    }

    /**
     * Question words and other glue that would otherwise match every section equally.
     * Deliberately small: this is word overlap, not real language understanding, so
     * over-filtering just throws away signal.
     */
    private val stopwords = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "do", "does", "did", "can", "could", "would", "should", "will",
        "what", "why", "when", "where", "which", "who", "whom", "how",
        "of", "in", "on", "at", "to", "for", "and", "or", "but", "if",
        "it", "its", "this", "that", "these", "those",
        "you", "your", "yours", "me", "my", "i", "we", "us",
        "please", "tell", "explain", "about", "so", "just", "again"
    )

    /**
     * Splits [text] into lowercase word tokens and strips a few common suffixes so
     * "sorting"/"sorted"/"sorts" all collapse to the same key as "sort". Cheap on
     * purpose — this only has to line up with the small, hand-written keyword lists
     * in [LessonData], not do general stemming.
     */
    private fun tokenize(text: String): List<String> =
        Regex("[a-zA-Z']+").findAll(text)
            .map { it.value.lowercase() }
            .filter { it.length > 1 && it !in stopwords }
            .map { stem(it) }
            .toList()

    private fun stem(word: String): String = when {
        word.endsWith("ing") && word.length > 5 -> word.dropLast(3)
        word.endsWith("ed") && word.length > 4 -> word.dropLast(2)
        word.endsWith("es") && word.length > 4 -> word.dropLast(2)
        word.endsWith("s") && !word.endsWith("ss") && word.length > 3 -> word.dropLast(1)
        else -> word
    }

    /** [LessonSection]'s own vocabulary, pre-stemmed once: title (weighted) + keywords + body. */
    private data class SectionIndex(val section: LessonSection, val titleWords: Set<String>, val otherWords: Set<String>)

    private val index: List<SectionIndex> by lazy {
        LessonData.binarySearchLesson.sections.map { section ->
            SectionIndex(
                section = section,
                titleWords = tokenize(section.title).toSet(),
                otherWords = (tokenize(section.keywords.joinToString(" ")) +
                    section.units.flatMap { tokenize(it.text) }).toSet()
            )
        }
    }

    /** A title word is a more deliberate signal ("sorting?") than an incidental body word. */
    private const val TITLE_WORD_WEIGHT = 2
    private const val OTHER_WORD_WEIGHT = 1

    /** Below this, a "best" section is really just noise — send the fallback instead. */
    private const val MIN_SCORE = 2

    /** The section body read out when [answerFor] finds a confident match. */
    private fun spokenTextFor(section: LessonSection): String =
        section.units.joinToString(" ") { it.text }

    /**
     * Picks a section for [question] and returns the full spoken reply, including
     * [PREFACE]. Always returns something sayable — a generic pointer at the lesson's
     * topics when nothing scores highly enough to trust.
     */
    fun answerFor(question: String): String {
        val words = tokenize(question)
        if (words.isEmpty()) return fallbackText

        var best: SectionIndex? = null
        var bestScore = 0
        for (candidate in index) {
            var score = 0
            for (word in words) {
                if (word in candidate.titleWords) score += TITLE_WORD_WEIGHT
                else if (word in candidate.otherWords) score += OTHER_WORD_WEIGHT
            }
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }

        val section = best?.section ?: return fallbackText
        if (bestScore < MIN_SCORE) return fallbackText
        return PREFACE + spokenTextFor(section)
    }
}
