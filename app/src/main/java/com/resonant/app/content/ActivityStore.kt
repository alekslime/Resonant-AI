package com.resonant.app.content

import android.content.Context
import java.time.LocalDate

/**
 * Keeps the [ActivityLog] on the phone. Listening arrives a second at a time, so it is held in memory and written
 * every [FLUSH_EVERY] seconds, and once more when the app stops ([flush]). Lessons and quizzes are written at once.
 * The rules are in [Streaks]; this only loads, saves and asks what day it is.
 */
class ActivityStore(
    context: Context,
    private val today: () -> Int = { LocalDate.now().toEpochDay().toInt() }
) {
    private val sp = context.applicationContext.getSharedPreferences("resonant_activity", Context.MODE_PRIVATE)
    private var log: ActivityLog = read()
    private var unsaved = 0

    @Synchronized fun snapshot(): ActivityLog = log

    /** One second of speech has played. */
    @Synchronized fun tick() {
        log = Streaks.withListening(log, 1, today())
        unsaved++
        if (unsaved >= FLUSH_EVERY) write()
    }

    @Synchronized fun flush() {
        if (unsaved > 0) write()
    }

    /** The last sentence of a lesson was reached. Counts once per lesson; the day counts every time. */
    @Synchronized fun lessonFinished(id: String) {
        val next = Streaks.withLessonFinished(log, id, today())
        if (next != log) {
            log = next
            write()
        }
    }

    @Synchronized fun quizFinished() {
        val next = Streaks.withDayActive(log, today())
        if (next != log) {
            log = next
            write()
        }
    }

    private fun write() {
        sp.edit()
            .putStringSet(KEY_DAYS, log.activeDays.map { it.toString() }.toSet())
            .putLong(KEY_SECONDS, log.listeningSeconds)
            .putInt(KEY_TODAY_DAY, log.todayDay)
            .putInt(KEY_TODAY_SECONDS, log.todaySeconds)
            .putStringSet(KEY_LESSONS, log.lessonsFinished.toSet())
            .apply()
        unsaved = 0
    }

    private fun read(): ActivityLog = ActivityLog(
        activeDays = sp.getStringSet(KEY_DAYS, emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet(),
        listeningSeconds = sp.getLong(KEY_SECONDS, 0L),
        todayDay = sp.getInt(KEY_TODAY_DAY, -1),
        todaySeconds = sp.getInt(KEY_TODAY_SECONDS, 0),
        lessonsFinished = sp.getStringSet(KEY_LESSONS, emptySet())!!.toSet()
    )

    private companion object {
        const val FLUSH_EVERY = 15
        const val KEY_DAYS = "active_days"
        const val KEY_SECONDS = "listening_seconds"
        const val KEY_TODAY_DAY = "today_day"
        const val KEY_TODAY_SECONDS = "today_seconds"
        const val KEY_LESSONS = "lessons_finished"
    }
}
