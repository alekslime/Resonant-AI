package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreContentTest {

    private val lessons = LessonData.allLessons
    private val quizzes = QuizData.allQuizSets

    @Test
    fun new_content_is_part_of_the_library() {
        assertTrue(lessons.containsAll(MoreContent.all))
        assertTrue(quizzes.containsAll(MoreContent.allQuizzes))
    }

    @Test
    fun ids_are_unique() {
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
        assertEquals(quizzes.size, quizzes.map { it.id }.toSet().size)
        val questionIds = quizzes.flatMap { it.questions }.map { it.id }
        assertEquals(questionIds.size, questionIds.toSet().size)
    }

    @Test
    fun every_question_is_well_formed() {
        for (set in quizzes) for (q in set.questions) {
            assertTrue("${q.id} has too few options", q.options.size >= 3)
            assertTrue("${q.id} correctIndex", q.correctIndex in q.options.indices)
            assertEquals("${q.id} letters", q.options.indices.map { 'A' + it }, q.options.map { it.letter })
            assertEquals("${q.id} repeats an option", q.options.size, q.options.map { it.text }.toSet().size)
            assertTrue("${q.id} has no explanation", q.explanation.isNotBlank())
        }
    }

    @Test
    fun the_right_answer_is_not_always_in_the_same_place() {
        val positions = MoreContent.allQuizzes.flatMap { it.questions }.map { it.correctIndex }.toSet()
        assertTrue(positions.size >= 3)
    }

    @Test
    fun everything_read_aloud_is_plain_text() {
        val spoken = lessons.flatMap { l -> l.sections.flatMap { s -> listOf(s.title) + s.units.map { it.text } } } +
            quizzes.flatMap { s -> listOf(s.title) + s.questions.flatMap { q -> listOf(q.prompt.text, q.explanation) + q.options.map { it.text } } }
        for (t in spoken) {
            assertTrue("blank text", t.isNotBlank())
            assertFalse("\"$t\" has markdown", t.any { it in "*#`_" })
        }
    }

    @Test
    fun every_new_lesson_has_five_sections_with_a_summary_and_keywords() {
        for (l in MoreContent.all) {
            assertEquals(l.id, 5, l.sections.size)
            assertEquals("Key takeaway", l.sections.last().title)
            for (s in l.sections) assertTrue("${l.id}/${s.id}", s.keywords.isNotEmpty() && s.units.size >= 3)
        }
    }

    @Test
    fun a_question_names_its_own_new_lesson() {
        fun title(q: String) = OfflineAnswers.answer(q, lessons).sentences.getOrNull(1)
        assertEquals("Evaporation.", title("tell me about evaporation"))
        assertEquals("Equivalent fractions.", title("what are equivalent fractions"))
        assertEquals("Pitch and volume.", title("explain pitch"))
    }
}
