package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAnswersTest {

    private val lesson = LessonData.binarySearchLesson

    /** The section a question was answered from, by the title that is spoken second. */
    private fun sectionFor(question: String): String? {
        val answer = OfflineAnswers.answer(question)
        return if (answer.matched) answer.sentences[1] else null
    }

    // ---- routing: each kind of question finds the section a person would expect

    @Test
    fun a_what_is_question_gets_the_introduction() {
        assertEquals("What is binary search?", sectionFor("What is binary search?"))
        assertEquals("What is binary search?", sectionFor("what does binary search mean"))
    }

    @Test
    fun a_how_does_it_work_question_gets_the_algorithm_not_the_introduction() {
        // The words "binary search" appear all through the lesson; only "work" says which part.
        assertEquals("The algorithm.", sectionFor("How does binary search work?"))
        assertEquals("The algorithm.", sectionFor("What are the steps?"))
    }

    @Test
    fun a_why_sorted_question_gets_the_sorting_section() {
        assertEquals("Why sorting matters.", sectionFor("Why does the list need to be sorted?"))
        assertEquals("Why sorting matters.", sectionFor("does it work on an unsorted list"))
    }

    @Test
    fun an_example_request_gets_the_example() {
        assertEquals("Example.", sectionFor("Give me an example"))
    }

    @Test
    fun a_summary_request_gets_the_takeaway() {
        assertEquals("Key takeaway.", sectionFor("Can you summarize it?"))
    }

    @Test
    fun a_question_made_only_of_the_lessons_own_words_gets_the_start_of_the_lesson() {
        assertEquals("What is binary search?", sectionFor("Tell me about binary search"))
        assertEquals("What is binary search?", sectionFor("explain binary search"))
        assertEquals("What is binary search?", sectionFor("binary search"))
    }

    @Test
    fun casing_punctuation_and_plurals_do_not_matter() {
        assertEquals("Example.", sectionFor("GIVE ME AN EXAMPLE!!!"))
        assertEquals("Example.", sectionFor("any examples?"))
        assertEquals("Why sorting matters.", sectionFor("sorting, sorted, sorts"))
    }

    // ---- honesty: no match means saying so, not reading out something unrelated

    @Test
    fun off_topic_questions_are_not_answered_with_the_lesson() {
        assertEquals(null, sectionFor("How do I bake bread?"))
        assertEquals(null, sectionFor("hello"))
        assertEquals(null, sectionFor(""))
        assertEquals(null, sectionFor("   ?!  "))
    }

    @Test
    fun a_question_that_shares_only_a_topic_word_with_the_lesson_is_not_answered_with_it() {
        // "binary" is in the lesson's title, but a binary TREE is not binary SEARCH.
        assertEquals(null, sectionFor("Tell me about binary trees"))
        assertEquals(null, sectionFor("What is the time complexity?"))
    }

    @Test
    fun a_miss_says_what_it_can_answer_about() {
        val answer = OfflineAnswers.answer("How do I bake bread?")
        assertFalse(answer.matched)
        assertTrue(answer.sentences.first().contains("don't have a lesson on that"))
        assertTrue(answer.sentences.last().contains("Binary Search"))
        assertFalse("must not read the prefix twice", answer.sentences.last().contains("Introduction to"))
    }

    @Test
    fun a_miss_with_no_lessons_at_all_still_says_something() {
        val answer = OfflineAnswers.answer("anything", lessons = emptyList())
        assertFalse(answer.matched)
        assertTrue(answer.sentences.last().contains("no lessons"))
    }

    // ---- what is spoken

    @Test
    fun a_match_says_where_it_comes_from_then_the_title_then_the_section() {
        val answer = OfflineAnswers.answer("Give me an example")
        val section = lesson.sections.first { it.title == "Example" }
        assertTrue(answer.matched)
        assertEquals("I can't reach the AI server, so this is from the lesson.", answer.sentences[0])
        assertEquals("Example.", answer.sentences[1])
        assertEquals(section.units.map { it.text }, answer.sentences.drop(2))
    }

    @Test
    fun a_title_that_is_already_a_question_is_not_given_a_full_stop() {
        assertEquals("What is binary search?", OfflineAnswers.answer("what is binary search").sentences[1])
    }

    @Test
    fun nothing_spoken_contains_symbols_that_read_badly_aloud() {
        val questions = listOf("What is binary search?", "How does it work", "example", "summary", "bread")
        for (q in questions) for (s in OfflineAnswers.answer(q).sentences) {
            assertFalse("\"$s\" has markdown", s.contains('*') || s.contains('#') || s.contains('`') || s.contains('_'))
            assertTrue("\"$s\" is empty", s.isNotBlank())
        }
    }

    // ---- ties, several lessons, and the stemmer

    @Test
    fun the_same_question_always_gets_the_same_answer() {
        val first = OfflineAnswers.answer("How does binary search work?")
        repeat(20) { assertEquals(first, OfflineAnswers.answer("How does binary search work?")) }
    }

    @Test
    fun with_several_lessons_the_one_the_question_names_wins() {
        fun unit(t: String) = SemanticUnit("u", t)
        val bubble = Lesson(
            "l2", "Introduction to Bubble Sort",
            listOf(LessonSection("b1", "The algorithm", listOf(unit("Swap neighbours that are out of order.")), listOf("steps", "work")))
        )
        val both = listOf(lesson, bubble)
        assertEquals("The algorithm.", OfflineAnswers.answer("how does bubble sort work", both).sentences[1])
        // ...and the binary search lesson's algorithm, when that is what is named.
        val answer = OfflineAnswers.answer("how does binary search work", both)
        assertEquals("Start by looking at the middle item of the list.", answer.sentences[2])
    }

    @Test
    fun the_stemmer_folds_word_forms_together_without_mangling_short_words() {
        assertEquals("sort", OfflineAnswers.stem("sorted"))
        assertEquals("sort", OfflineAnswers.stem("sorting"))
        assertEquals("sort", OfflineAnswers.stem("sorts"))
        assertEquals("example", OfflineAnswers.stem("examples"))
        assertEquals("example", OfflineAnswers.stem("example"))
        assertEquals("search", OfflineAnswers.stem("searches"))
        assertEquals("process", OfflineAnswers.stem("process"))
        assertEquals("red", OfflineAnswers.stem("red"))
        assertEquals("bed", OfflineAnswers.stem("bed"))
        assertEquals("bus", OfflineAnswers.stem("bus"))
    }

    @Test
    fun question_words_and_filler_are_not_search_terms() {
        assertEquals(listOf("binary", "search"), OfflineAnswers.tokens("What is the binary search?"))
        assertEquals(emptyList<String>(), OfflineAnswers.tokens("How do I tell me please"))
    }

    // ---- the content itself

    @Test
    fun every_section_of_every_lesson_can_be_reached_by_some_question() {
        // Guards the keyword lists: a section nobody can ask for is dead content.
        val asks = mapOf(
            "What is binary search?" to "define it",
            "The algorithm." to "what are the steps",
            "Why sorting matters." to "why sorted",
            "Example." to "an example please",
            "Key takeaway." to "give me the summary"
        )
        val reached = asks.values.mapNotNull { sectionFor(it) }.toSet()
        for (section in lesson.sections) {
            val spoken = if (section.title.last() in ".?!") section.title else section.title + "."
            assertTrue("no question reaches \"${section.title}\"", spoken in reached)
        }
    }

    @Test
    fun keywords_do_not_contain_question_words_that_would_match_everything() {
        val banned = setOf("what", "how", "why", "when", "where", "who", "which")
        for (l in LessonData.allLessons) for (s in l.sections) for (k in s.keywords) {
            assertFalse("\"$k\" in ${s.id}", k.lowercase() in banned)
        }
    }
}
