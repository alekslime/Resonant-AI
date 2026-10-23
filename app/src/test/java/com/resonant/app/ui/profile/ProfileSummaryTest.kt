package com.resonant.app.ui.profile

import com.resonant.app.content.ActivityLog
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
        assertEquals(10, BrailleAvatar.cells("abcdefghij").size)
        assertEquals(44f, BrailleAvatar.cellWidthDp(3, 320f), 0.001f)
        val w = BrailleAvatar.cellWidthDp(20, 320f)
        assertTrue(w < 44f && w * 20 + w * (16f / 44f) * 19 <= 320.01f)
        assertTrue(BrailleAvatar.cells("").isEmpty())
        assertTrue(BrailleAvatar.cells(null).isEmpty())
    }

    // ---- streaks, lessons, listening ----

    @Test
    fun stats_carry_the_streak_the_listening_time_and_the_lessons_that_still_exist() {
        val activity = ActivityLog(
            activeDays = setOf(98, 99, 100, 40, 41),
            listeningSeconds = 3700,
            lessonsFinished = setOf("intro", "removed")
        )
        val s = ProfileSummary.stats(
            progress = emptyMap(),
            quizIds = ids,
            materials = 0,
            activity = activity,
            today = 100,
            lessonIds = listOf("intro", "sorting", "search")
        )
        assertEquals(3, s.streak)
        assertEquals(3, s.longestStreak)
        assertEquals(3700L, s.listeningSeconds)
        assertEquals(1, s.lessonsDone)     // "removed" no longer exists, so it does not count
        assertEquals(3, s.lessonsTotal)
    }

    @Test
    fun lessons_line() {
        assertEquals("Lessons: 2 of 7 finished", ProfileSummary.lessonsLine(ProfileStats(0, 0, null, 0, lessonsDone = 2, lessonsTotal = 7)))
        assertEquals("Lessons: none yet", ProfileSummary.lessonsLine(ProfileStats(0, 0, null, 0)))
    }

    @Test
    fun spoken_summary_mentions_streak_lessons_and_listening_when_there_are_any() {
        val s = ProfileStats(4, 4, 90, 1, streak = 3, longestStreak = 5, listeningSeconds = 5400, lessonsDone = 2, lessonsTotal = 6)
        val text = ProfileSummary.spoken("Aleks", s)
        assertTrue(text.contains("You are on a 3 day streak."))
        assertTrue(text.contains("You have finished 2 of 6 lessons."))
        assertTrue(text.contains("You have listened for 1 hour 30 minutes."))
        assertTrue(text.endsWith("Next, open a lesson you have not finished."))
    }

    @Test
    fun spoken_summary_leaves_out_what_does_not_exist_yet() {
        val text = ProfileSummary.spoken(null, ProfileStats(0, 0, null, 0))
        assertTrue(!text.contains("streak"))
        assertTrue(!text.contains("listened"))
        assertTrue(!text.contains("lesson"))
    }

    @Test
    fun a_quiz_still_comes_before_a_lesson_as_the_next_step() {
        val s = ProfileStats(1, 3, 50, 2, lessonsDone = 0, lessonsTotal = 4)
        assertTrue(ProfileSummary.spoken("A", s).endsWith("Next, try a quiz you have not finished."))
    }

    @Test
    fun the_shimmer_travels_along_the_name_and_leaves_it_dark_at_both_ends() {
        assertEquals(0f, AvatarMotion.shine(0f, 3, 5), 0.0001f)
        assertEquals(0f, AvatarMotion.shine(1f, 0, 5), 0.0001f)
        // Under the light, a cell is at its brightest; a far one is dark.
        val best = (0..4).maxByOrNull { AvatarMotion.shine(0.5f, it, 5) }!!
        assertEquals(1f, AvatarMotion.shine(0.5f, best, 5), 0.5f)
        assertEquals(0f, AvatarMotion.shine(0.5f, 0, 0), 0.0001f)
    }
}

class ProfileMotionTest {

    @Test
    fun numbers_climb_from_zero_and_the_finished_label_is_untouched() {
        assertEquals("Quizzes: 0 of 0 done", CountUp.apply("Quizzes: 3 of 8 done", 0f))
        assertEquals("Quizzes: 2 of 4 done", CountUp.apply("Quizzes: 3 of 8 done", 0.5f))
        assertEquals("Quizzes: 3 of 8 done", CountUp.apply("Quizzes: 3 of 8 done", 1f))
        assertEquals("Streak: none yet", CountUp.apply("Streak: none yet", 0.3f))
    }

    @Test
    fun a_bad_progress_never_breaks_the_label() {
        assertEquals("Materials: 0", CountUp.apply("Materials: 5", -2f))
        assertEquals("Materials: 5", CountUp.apply("Materials: 5", 7f))
    }

    @Test
    fun dots_arrive_in_order_and_every_dot_ends_fully_in_at_normal_size() {
        assertTrue(AvatarMotion.dotProgress(0.3f, 1) > AvatarMotion.dotProgress(0.3f, 4))
        for (dot in 1..6) {
            assertEquals(1f, AvatarMotion.dotProgress(1f, dot), 0.0001f)
            assertEquals(0f, AvatarMotion.dotProgress(0f, dot), 0.0001f)
        }
        assertEquals(1f, AvatarMotion.swell(0f), 0.0001f)
        assertEquals(1f, AvatarMotion.swell(1f), 0.0001f)
        assertEquals(1f + AvatarMotion.SWELL, AvatarMotion.swell(0.5f), 0.0001f)
    }

    @Test
    fun resting_dots_are_faint_and_settled_by_a_quarter_of_the_way() {
        assertEquals(0f, AvatarMotion.restingAlpha(0f), 0.0001f)
        assertEquals(0.15f, AvatarMotion.restingAlpha(0.25f), 0.0001f)
        assertEquals(0.15f, AvatarMotion.restingAlpha(1f), 0.0001f)
    }
}
