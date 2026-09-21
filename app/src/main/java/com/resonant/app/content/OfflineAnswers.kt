package com.resonant.app.content

/**
 * What Chat says when the AI server can't be used: an answer taken from the lessons
 * that ship inside the app, so a dead server or the wrong Wi-Fi degrades the demo
 * instead of ending it.
 *
 * Deliberately simple and honest. This is a keyword match over lesson text, not
 * understanding: it finds the section that shares the most words with the question and
 * reads it out, and it says so ("this is from the lesson"). When nothing matches well
 * enough it admits that, rather than reading out something unrelated, and tells the
 * person what it CAN answer about.
 *
 * Pure Kotlin, no Android, so all of it is unit tested on the JVM.
 */
object OfflineAnswers {

    /** Sentences to speak, in order, and whether a lesson section was actually found. */
    data class Answer(val sentences: List<String>, val matched: Boolean)

    // A word counts more the more deliberately it points at a section: an author-chosen
    // keyword beats a word in the title, which beats a word somewhere in the body.
    private const val KEYWORD_WEIGHT = 4
    private const val TITLE_WEIGHT = 3
    private const val BODY_WEIGHT = 1

    /**
     * Added per word of the lesson's own title that the question uses, so with several
     * lessons the one the question names beats one it merely brushes against. Only ever
     * added to a section that already has a real hit: topic words alone match nothing.
     */
    private const val TOPIC_BONUS = 1

    /**
     * Below this the match is a coincidence: one title word alone ("matters") is not
     * enough, one keyword ("example") is.
     */
    private const val MIN_SCORE = 4

    private const val PREFACE = "I can't reach the AI server, so this is from the lesson."

    // Question and request words are dropped: "what", "how" and "explain" are in almost
    // every question, so they say nothing about WHICH section is wanted.
    private val STOPWORDS = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "am",
        "do", "does", "did", "of", "to", "in", "on", "for", "and", "or",
        "it", "its", "this", "that", "these", "those", "me", "my", "i", "you", "your",
        "can", "could", "would", "should", "will", "please", "let", "lets", "us",
        "with", "at", "by", "from", "as", "so", "if", "then", "than", "there", "here",
        "some", "any", "just", "also", "very", "much",
        "what", "how", "why", "when", "where", "who", "which", "about",
        "tell", "give", "explain", "describe", "talk", "say", "know", "want", "like"
    )

    fun answer(question: String, lessons: List<Lesson> = LessonData.allLessons): Answer {
        val wanted = tokens(question).toSet()

        var best: LessonSection? = null
        var bestScore = 0
        var topicLesson: Lesson? = null

        if (wanted.isNotEmpty()) {
            for (lesson in lessons) {
                // The lesson title's own words ("binary", "search") appear all through the
                // lesson, so they can't say which section is wanted. Score on everything else.
                val topic = tokens(lesson.title).toSet()
                val shared = wanted.count { it in topic }
                val distinct = wanted - topic

                if (distinct.isEmpty() && shared > 0 && topicLesson == null) topicLesson = lesson

                for (section in lesson.sections) {
                    val raw = score(section, distinct)
                    val score = if (raw > 0) raw + shared * TOPIC_BONUS else 0
                    // Strictly better only, so on a tie the earlier section wins.
                    if (score > bestScore) {
                        best = section
                        bestScore = score
                    }
                }
            }
        }

        val section = when {
            best != null && bestScore >= MIN_SCORE -> best
            // A question made only of the lesson's own words ("binary search", "tell me
            // about binary search") is asking for the lesson itself: start at the top.
            topicLesson != null -> topicLesson.sections.firstOrNull()
            else -> null
        }

        if (section == null) {
            return Answer(
                sentences = listOf(
                    "I can't reach the AI server, and I don't have a lesson on that.",
                    topicsLine(lessons)
                ),
                matched = false
            )
        }
        return Answer(
            sentences = listOf(PREFACE, asSentence(section.title)) + section.units.map { it.text },
            matched = true
        )
    }

    private fun score(section: LessonSection, wanted: Set<String>): Int {
        val weights = HashMap<String, Int>()
        fun add(words: List<String>, weight: Int) {
            for (w in words) if ((weights[w] ?: 0) < weight) weights[w] = weight
        }
        add(section.units.flatMap { tokens(it.text) }, BODY_WEIGHT)
        add(tokens(section.title), TITLE_WEIGHT)
        add(section.keywords.flatMap { tokens(it) }, KEYWORD_WEIGHT)
        return wanted.sumOf { weights[it] ?: 0 }
    }

    private fun topicsLine(lessons: List<Lesson>): String {
        val topics = lessons.map { it.title.removePrefix("Introduction to ") }
        return when (topics.size) {
            0 -> "There are no lessons to answer from yet."
            1 -> "Try asking about ${topics[0]}."
            else -> "Try asking about ${topics.dropLast(1).joinToString(", ")} or ${topics.last()}."
        }
    }

    /** Titles are spoken as a sentence of their own; give them closing punctuation. */
    private fun asSentence(title: String): String =
        if (title.last() in ".?!") title else "$title."

    /** Lowercased words with the boring ones dropped and plurals/tenses folded together. */
    internal fun tokens(text: String): List<String> =
        Regex("[a-z0-9]+").findAll(text.lowercase())
            .map { it.value }
            .filter { it.length > 1 && it !in STOPWORDS }
            .map { stem(it) }
            .toList()

    /**
     * A light stemmer, just enough that "sorted", "sorting" and "sorts" all meet at
     * "sort". It only strips when at least three letters remain, and it does not touch
     * "-es" except after ch/sh/ss/x/z, because "examples" must become "example", not
     * "exampl".
     */
    internal fun stem(word: String): String {
        fun strip(suffix: String): String? =
            if (word.endsWith(suffix) && word.length - suffix.length >= 3) word.dropLast(suffix.length) else null

        strip("ing")?.let { return it }
        strip("ed")?.let { return it }
        if (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses") ||
            word.endsWith("xes") || word.endsWith("zes")
        ) {
            strip("es")?.let { return it }
        }
        if (!word.endsWith("ss")) strip("s")?.let { return it }
        return word
    }
}
