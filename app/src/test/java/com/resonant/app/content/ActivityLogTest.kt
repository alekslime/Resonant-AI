package com.resonant.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityLogTest {

    // ---- streak ----

    @Test
    fun no_days_means_no_streak() {
        assertEquals(0, Streaks.current(emptySet(), today = 100))
        assertEquals(0, Streaks.longest(emptySet()))
    }

    @Test
    fun a_run_that_ends_today_counts_every_day() {
        assertEquals(1, Streaks.current(setOf(100), 100))
        assertEquals(3, Streaks.current(setOf(98, 99, 100), 100))
    }

    @Test
    fun a_streak_is_still_alive_on_a_day_with_nothing_yet() {
        assertEquals(3, Streaks.current(setOf(97, 98, 99), 100))
    }

    @Test
    fun a_missed_day_ends_the_streak() {
        assertEquals(0, Streaks.current(setOf(96, 97, 98), 100))
        assertEquals(1, Streaks.current(setOf(90, 91, 100), 100))
    }

    @Test
    fun the_run_is_cut_at_a_gap() {
        assertEquals(3, Streaks.current(setOf(95, 98, 99, 100), 100))
    }

    @Test
    fun longest_finds_the_best_run_anywhere() {
        assertEquals(3, Streaks.longest(setOf(1, 2, 3, 7, 8)))
        assertEquals(1, Streaks.longest(setOf(5)))
        assertEquals(4, Streaks.longest(setOf(10, 11, 12, 13, 20, 21)))
    }

    // ---- listening ----

    @Test
    fun a_day_becomes_active_after_a_minute_of_listening() {
        var log = ActivityLog()
        log = Streaks.withListening(log, 59, today = 100)
        assertFalse(100 in log.activeDays)
        log = Streaks.withListening(log, 1, 100)
        assertTrue(100 in log.activeDays)
        assertEquals(60L, log.listeningSeconds)
    }

    @Test
    fun a_new_day_starts_its_count_again_but_total_listening_keeps_growing() {
        var log = Streaks.withListening(ActivityLog(), 50, today = 100)
        log = Streaks.withListening(log, 50, today = 101)
        assertEquals(100L, log.listeningSeconds)
        assertEquals(50, log.todaySeconds)
        assertTrue(log.activeDays.isEmpty())   // 50 seconds on each of two days is not a day of learning
    }

    @Test
    fun listening_zero_or_less_changes_nothing() {
        val log = ActivityLog(listeningSeconds = 10)
        assertEquals(log, Streaks.withListening(log, 0, 100))
        assertEquals(log, Streaks.withListening(log, -5, 100))
    }

    // ---- days, lessons ----

    @Test
    fun finishing_a_lesson_marks_the_day_and_remembers_the_lesson_once() {
        var log = Streaks.withLessonFinished(ActivityLog(), "intro", today = 100)
        assertEquals(setOf("intro"), log.lessonsFinished)
        assertTrue(100 in log.activeDays)
        log = Streaks.withLessonFinished(log, "intro", 101)
        assertEquals(setOf("intro"), log.lessonsFinished)
        assertEquals(setOf(100, 101), log.activeDays)
    }

    @Test
    fun marking_a_day_twice_is_the_same_as_once() {
        val once = Streaks.withDayActive(ActivityLog(), 100)
        assertEquals(once, Streaks.withDayActive(once, 100))
    }

    @Test
    fun very_old_days_are_forgotten() {
        val log = Streaks.withDayActive(ActivityLog(activeDays = setOf(1, 2, 3)), today = 1000)
        assertEquals(setOf(1000), log.activeDays)
        assertEquals(setOf(700, 1000), Streaks.trim(setOf(700, 1000, 100), 1000))
    }

    // ---- wording ----

    @Test
    fun streak_and_longest_lines() {
        assertEquals("Streak: none yet", ActivityText.streakLine(0))
        assertEquals("Streak: 1 day", ActivityText.streakLine(1))
        assertEquals("Streak: 5 days", ActivityText.streakLine(5))
        assertEquals("Longest streak: 12 days", ActivityText.longestLine(12))
    }

    @Test
    fun listening_time_is_said_in_the_right_unit() {
        assertEquals("under a minute", ActivityText.listening(0))
        assertEquals("under a minute", ActivityText.listening(59))
        assertEquals("1 minute", ActivityText.listening(60))
        assertEquals("42 minutes", ActivityText.listening(42 * 60L))
        assertEquals("1 hour", ActivityText.listening(3600))
        assertEquals("1 hour 1 minute", ActivityText.listening(3660))
        assertEquals("2 hours 5 minutes", ActivityText.listening(2 * 3600 + 5 * 60L))
        assertEquals("Listening: 2 hours", ActivityText.listeningLine(7200))
    }
}
