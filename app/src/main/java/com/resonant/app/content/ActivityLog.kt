package com.resonant.app.content

/**
 * What the person has done, kept as plain values so the rules can be unit tested. Days are epoch days (whole days
 * since 1970 in the phone's own time zone), which makes "yesterday" just `today - 1`.
 */
data class ActivityLog(
    /** Days that count as a day of learning. */
    val activeDays: Set<Int> = emptySet(),
    /** Every second of speech listened to, ever. */
    val listeningSeconds: Long = 0L,
    /** The day [todaySeconds] belongs to; a new day starts it again from zero. */
    val todayDay: Int = -1,
    val todaySeconds: Int = 0,
    /** Ids of the lessons read to the last sentence. */
    val lessonsFinished: Set<String> = emptySet()
)

object Streaks {
    /** A day counts once this much has been listened to, or a lesson or quiz is finished. */
    const val ACTIVE_SECONDS = 60

    /** Older days are forgotten; a streak that long is shown the same either way. */
    const val KEEP_DAYS = 400

    /**
     * Days in a row ending today. A streak is still alive on a day with nothing yet, as long as yesterday
     * counted, so opening the app in the morning does not show a broken streak.
     */
    fun current(days: Set<Int>, today: Int): Int {
        var day = when {
            today in days -> today
            today - 1 in days -> today - 1
            else -> return 0
        }
        var run = 0
        while (day in days) {
            run++
            day--
        }
        return run
    }

    fun longest(days: Set<Int>): Int {
        var best = 0
        var run = 0
        var previous: Int? = null
        for (day in days.sorted()) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return best
    }

    /** Adds [seconds] of listening. The day becomes active once it has had [ACTIVE_SECONDS] of it. */
    fun withListening(log: ActivityLog, seconds: Int, today: Int): ActivityLog {
        if (seconds <= 0) return log
        val sameDay = if (log.todayDay == today) log else log.copy(todayDay = today, todaySeconds = 0)
        val todaySeconds = sameDay.todaySeconds + seconds
        val next = sameDay.copy(
            listeningSeconds = sameDay.listeningSeconds + seconds,
            todaySeconds = todaySeconds
        )
        return if (todaySeconds >= ACTIVE_SECONDS) withDayActive(next, today) else next
    }

    fun withDayActive(log: ActivityLog, today: Int): ActivityLog =
        if (today in log.activeDays) log
        else log.copy(activeDays = trim(log.activeDays + today, today))

    fun withLessonFinished(log: ActivityLog, id: String, today: Int): ActivityLog =
        withDayActive(log.copy(lessonsFinished = log.lessonsFinished + id), today)

    fun trim(days: Set<Int>, today: Int): Set<Int> = days.filter { it > today - KEEP_DAYS }.toSet()
}

/** How these numbers are said and shown. One place, so the screen and the spoken summary agree. */
object ActivityText {
    fun days(n: Int): String = if (n == 1) "1 day" else "$n days"

    fun streakLine(streak: Int): String =
        if (streak <= 0) "Streak: none yet" else "Streak: ${days(streak)}"

    fun longestLine(longest: Int): String = "Longest streak: ${days(longest)}"

    fun listening(seconds: Long): String {
        val minutes = (seconds / 60).toInt()
        return when {
            seconds < 60 -> "under a minute"
            minutes < 60 -> if (minutes == 1) "1 minute" else "$minutes minutes"
            else -> {
                val hours = minutes / 60
                val rest = minutes % 60
                val h = if (hours == 1) "1 hour" else "$hours hours"
                if (rest == 0) h else "$h ${if (rest == 1) "1 minute" else "$rest minutes"}"
            }
        }
    }

    fun listeningLine(seconds: Long): String = "Listening: ${listening(seconds)}"
}
