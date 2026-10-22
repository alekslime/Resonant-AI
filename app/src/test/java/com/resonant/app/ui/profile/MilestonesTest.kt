package com.resonant.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestonesTest {

    private fun stats(
        quizzesDone: Int = 0, quizzesTotal: Int = 4, materials: Int = 0, streak: Int = 0,
        longest: Int = 0, seconds: Long = 0, lessonsDone: Int = 0, lessonsTotal: Int = 10
    ) = ProfileStats(quizzesDone, quizzesTotal, null, materials, streak, longest, seconds, lessonsDone, lessonsTotal)

    private fun ids(list: List<Milestone>) = list.map { it.id }

    @Test
    fun a_fresh_start_has_nothing_reached_and_the_first_lesson_is_next() {
        assertTrue(Milestones.reachedNow(stats()).isEmpty())
        assertEquals("lessons_1", Milestones.next(emptyList())?.id)
    }

    @Test
    fun the_firsts_are_reached_by_one_of_each() {
        val s = stats(lessonsDone = 1, quizzesDone = 1, materials = 1)
        assertEquals(listOf("lessons_1", "quizzes_1", "material_1"), ids(Milestones.reachedNow(s)))
    }

    @Test
    fun a_streak_counts_even_after_it_broke() {
        val s = stats(streak = 0, longest = 7)
        assertEquals(listOf("streak_3", "streak_7"), ids(Milestones.reachedNow(s)))
    }

    @Test
    fun every_lesson_and_every_quiz_need_something_to_finish() {
        assertTrue(ids(Milestones.reachedNow(stats(lessonsDone = 0, lessonsTotal = 0, quizzesDone = 0, quizzesTotal = 0))).isEmpty())
        val s = stats(lessonsDone = 10, lessonsTotal = 10, quizzesDone = 4, quizzesTotal = 4)
        assertTrue("lessons_all" in ids(Milestones.reachedNow(s)))
        assertTrue("quizzes_all" in ids(Milestones.reachedNow(s)))
    }

    @Test
    fun listening_milestones_use_hours() {
        assertTrue(ids(Milestones.reachedNow(stats(seconds = 3599))).isEmpty())
        assertEquals(listOf("listen_1h"), ids(Milestones.reachedNow(stats(seconds = 3600))))
        assertEquals(listOf("listen_1h", "listen_10h"), ids(Milestones.reachedNow(stats(seconds = 36000))))
    }

    @Test
    fun only_what_was_not_told_before_is_fresh_and_a_told_one_never_un_reaches() {
        val s = stats(lessonsDone = 1, quizzesDone = 1)
        assertEquals(listOf("quizzes_1"), ids(Milestones.fresh(s, setOf("lessons_1"))))
        // A streak told about earlier still counts as reached when the numbers have since dropped.
        assertEquals(listOf("streak_3"), ids(Milestones.reached(stats(), setOf("streak_3"))))
        assertTrue(Milestones.fresh(stats(), setOf("streak_3")).isEmpty())
    }

    @Test
    fun the_ids_are_unique_so_remembering_them_is_safe() {
        assertEquals(Milestones.ALL.size, Milestones.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun the_row_line_says_how_many_and_how_many_are_new() {
        val total = Milestones.ALL.size
        assertEquals("Milestones: 0 of $total", Milestones.rowLine(emptyList(), emptyList()))
        val two = Milestones.ALL.take(2)
        assertEquals("Milestones: 2 of $total, 1 new", Milestones.rowLine(two, two.take(1)))
    }

    @Test
    fun spoken_row_names_what_was_reached_and_the_next_goal() {
        val text = Milestones.spoken(Milestones.ALL.take(2))
        assertTrue(text.contains("You have reached 2 of ${Milestones.ALL.size} milestones."))
        assertTrue(text.contains("Reached: First lesson finished, First quiz finished."))
        assertTrue(text.endsWith("Next goal: ${Milestones.ALL[2].title}."))
    }

    @Test
    fun a_long_list_is_not_read_out_and_having_everything_is_said() {
        assertTrue(!Milestones.spoken(Milestones.ALL.take(6)).contains("Reached:"))
        assertTrue(Milestones.spoken(Milestones.ALL).endsWith("You have reached every milestone."))
        assertNull(Milestones.next(Milestones.ALL))
    }

    @Test
    fun the_celebration_is_short_whatever_happened() {
        val a = Milestones.ALL
        assertEquals("", Milestones.announcement(emptyList()))
        assertEquals("New milestone: First lesson finished.", Milestones.announcement(a.take(1)))
        assertEquals(
            "New milestones: First lesson finished, First quiz finished and First material of your own.",
            Milestones.announcement(a.take(3))
        )
        val many = Milestones.announcement(a.take(6))
        assertTrue(many.startsWith("You have reached 6 new milestones, including "))
        assertTrue(many.length < 100)
    }
}
