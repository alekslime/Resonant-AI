package com.resonant.app.ui.profile

import com.resonant.app.content.ActivityLog
import com.resonant.app.content.ActivityText
import com.resonant.app.content.QuizProgressStore
import com.resonant.app.content.Streaks

/** What the Profile screen shows about progress. Plain numbers, so the wording is unit tested. */
internal data class ProfileStats(
    val quizzesDone: Int,
    val quizzesTotal: Int,
    /** Mean of each finished quiz's best score, in percent. Null until one is finished. */
    val averagePercent: Int?,
    val materials: Int,
    /** Days in a row with some learning, still alive if yesterday counted. */
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val listeningSeconds: Long = 0L,
    val lessonsDone: Int = 0,
    val lessonsTotal: Int = 0
)

internal object ProfileSummary {

    /** Counts only quizzes and lessons that exist now, so a removed one can't push a count past its total. */
    fun stats(
        progress: Map<String, QuizProgressStore.QuizProgress>,
        quizIds: List<String>,
        materials: Int,
        activity: ActivityLog = ActivityLog(),
        today: Int = 0,
        lessonIds: List<String> = emptyList()
    ): ProfileStats {
        val done = quizIds.mapNotNull { progress[it] }
            .filter { it.status == QuizProgressStore.Status.COMPLETED }
        val average = if (done.isEmpty()) null else done.sumOf { it.percentComplete } / done.size
        return ProfileStats(
            quizzesDone = done.size,
            quizzesTotal = quizIds.size,
            averagePercent = average,
            materials = materials,
            streak = Streaks.current(activity.activeDays, today),
            longestStreak = Streaks.longest(activity.activeDays),
            listeningSeconds = activity.listeningSeconds,
            lessonsDone = lessonIds.count { it in activity.lessonsFinished },
            lessonsTotal = lessonIds.size
        )
    }

    fun quizLine(s: ProfileStats): String =
        if (s.quizzesTotal == 0) "Quizzes: none yet" else "Quizzes: ${s.quizzesDone} of ${s.quizzesTotal} done"

    fun lessonsLine(s: ProfileStats): String =
        if (s.lessonsTotal == 0) "Lessons: none yet" else "Lessons: ${s.lessonsDone} of ${s.lessonsTotal} finished"

    fun averageLine(s: ProfileStats): String? =
        s.averagePercent?.let { "Average best score: $it percent" }

    fun materialsLine(s: ProfileStats): String =
        if (s.materials == 0) "Materials: none yet" else "Materials: ${s.materials}"

    /** The spoken "How am I doing?" answer. Says what is there and one thing to do next, never a verdict. */
    fun spoken(name: String?, s: ProfileStats): String {
        val who = name?.trim()?.takeIf { it.isNotEmpty() }
        val parts = mutableListOf<String>()
        parts += if (who != null) "Here is how you are doing, $who." else "Here is how you are doing."
        if (s.streak > 0) parts += "You are on a ${s.streak} day streak."
        if (s.lessonsTotal > 0) {
            parts += if (s.lessonsDone == 0) "You have not finished a lesson yet."
            else "You have finished ${s.lessonsDone} of ${s.lessonsTotal} lessons."
        }
        parts += when {
            s.quizzesTotal == 0 -> "There are no quizzes yet."
            s.quizzesDone == 0 -> "You have not finished a quiz yet."
            else -> "You have finished ${s.quizzesDone} of ${s.quizzesTotal} quizzes" +
                (s.averagePercent?.let { ", with an average best score of $it percent." } ?: ".")
        }
        parts += when (s.materials) {
            0 -> "You have not added your own material yet."
            1 -> "You have added one material of your own."
            else -> "You have added ${s.materials} materials of your own."
        }
        if (s.listeningSeconds >= 60) parts += "You have listened for ${ActivityText.listening(s.listeningSeconds)}."
        parts += when {
            s.quizzesTotal > 0 && s.quizzesDone < s.quizzesTotal -> "Next, try a quiz you have not finished."
            s.lessonsTotal > 0 && s.lessonsDone < s.lessonsTotal -> "Next, open a lesson you have not finished."
            s.materials == 0 -> "You can add your own material in Settings."
            else -> "Keep going."
        }
        return parts.joinToString(" ")
    }
}

/** The name written in braille, one cell per letter, used as the avatar. Unique per name without a photo. */
internal object BrailleAvatar {

    /** Dot numbers 1-6 for a to z. Dots 1-3 run down the left column, 4-6 down the right. */
    private val LETTERS = mapOf(
        'a' to "1", 'b' to "12", 'c' to "14", 'd' to "145", 'e' to "15", 'f' to "124", 'g' to "1245",
        'h' to "125", 'i' to "24", 'j' to "245", 'k' to "13", 'l' to "123", 'm' to "134", 'n' to "1345",
        'o' to "135", 'p' to "1234", 'q' to "12345", 'r' to "1235", 's' to "234", 't' to "2345",
        'u' to "136", 'v' to "1236", 'w' to "2456", 'x' to "1346", 'y' to "13456", 'z' to "1356"
    )

    const val MAX_CELLS = 5

    /** One list of the dot numbers that are raised per cell. Only plain a-z letters count; the rest are skipped. */
    fun cells(name: String?, max: Int = MAX_CELLS): List<List<Int>> =
        (name ?: "").lowercase()
            .mapNotNull { LETTERS[it] }
            .take(max)
            .map { dots -> dots.map { it - '0' } }
}
