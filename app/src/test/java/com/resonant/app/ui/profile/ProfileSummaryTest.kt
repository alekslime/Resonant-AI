package com.resonant.app.ui.profile

import com.resonant.app.content.QuizProgressStore.QuizProgress
import com.resonant.app.content.QuizProgressStore.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSummaryTest {

    private val ids = listOf("a", "b", "c")

    @Test
    fun counts_only_finished_quizzes_that_still_exist() {
        val progress = mapOf(
            "a" to QuizProgress(Status.COMPLETED, 4, 5),
            "b" to QuizProgress(Status.IN_PROGRESS, 0, 5),
            "gone" to QuizProgress(Status.COMPLETED, 5, 5)
        )
        val s = ProfileSummary.stats(progress, ids, materials = 2)
        assertEquals(1, s.quizzesDone)
        assertEquals(3, s.quizzesTotal)
        assertEquals(80, s.averagePercent)
        assertEquals(2, s.materials)
    }

    @Test
    fun no_average_until_a_quiz_is_finished() {
        val s = ProfileSummary.stats(emptyMap(), ids, 0)
        assertNull(s.averagePercent)
        assertNull(ProfileSummary.averageLine(s))
    }

    @Test
    fun lines_read_naturally() {
        val s = ProfileStats(2, 5, 70, 0)
        assertEquals("Quizzes: 2 of 5 done", ProfileSummary.quizLine(s))
        assertEquals("Average best score: 70 percent", ProfileSummary.averageLine(s))
        assertEquals("Materials: none yet", ProfileSummary.materialsLine(s))
        assertEquals("Quizzes: none yet", ProfileSummary.quizLine(ProfileStats(0, 0, null, 0)))
    }

    @Test
    fun spoken_summary_uses_the_name_and_suggests_one_next_step() {
        val text = ProfileSummary.spoken("Aleks", ProfileStats(1, 4, 60, 3))
        assertTrue(text.startsWith("Here is how you are doing, Aleks."))
        assertTrue(text.contains("1 of 4 quizzes"))
        assertTrue(text.contains("3 materials"))
        assertTrue(text.endsWith("Next, try a quiz you have not finished."))
    }

    @Test
    fun spoken_summary_works_with_nothing_yet_and_no_name() {
        val text = ProfileSummary.spoken(null, ProfileStats(0, 3, null, 0))
        assertTrue(text.startsWith("Here is how you are doing."))
        assertTrue(text.contains("not finished a quiz"))
    }

    @Test
    fun braille_uses_the_real_letter_patterns() {
        assertEquals(listOf(listOf(1), listOf(1, 2), listOf(1, 4)), BrailleAvatar.cells("Abc"))
        assertEquals(listOf(1, 2, 3, 5), BrailleAvatar.cells("R").single())
    }

    @Test
    fun braille_skips_non_letters_and_caps_the_length() {
        assertEquals(2, BrailleAvatar.cells("a-b!").size)
        assertEquals(BrailleAvatar.MAX_CELLS, BrailleAvatar.cells("abcdefghij").size)
        assertTrue(BrailleAvatar.cells("").isEmpty())
        assertTrue(BrailleAvatar.cells(null).isEmpty())
    }
}
