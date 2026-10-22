package com.resonant.app.ui.profile

/** One thing worth marking: a short title that is both shown and spoken, and the rule for having reached it. */
internal class Milestone(val id: String, val title: String, val reachedBy: (ProfileStats) -> Boolean)

/**
 * The milestones on Profile. Pure rules over [ProfileStats], so they are unit tested. The list is in the order
 * a person is likely to meet them: the firsts, then streaks, then the bigger counts.
 */
internal object Milestones {

    private fun streak(days: Int) =
        Milestone("streak_$days", "$days day streak") { maxOf(it.streak, it.longestStreak) >= days }

    val ALL: List<Milestone> = listOf(
        Milestone("lessons_1", "First lesson finished") { it.lessonsDone >= 1 },
        Milestone("quizzes_1", "First quiz finished") { it.quizzesDone >= 1 },
        Milestone("material_1", "First material of your own") { it.materials >= 1 },
        streak(3),
        streak(7),
        streak(14),
        streak(30),
        Milestone("lessons_5", "5 lessons finished") { it.lessonsDone >= 5 },
        Milestone("lessons_10", "10 lessons finished") { it.lessonsDone >= 10 },
        Milestone("lessons_all", "Every lesson finished") { it.lessonsTotal > 0 && it.lessonsDone >= it.lessonsTotal },
        Milestone("quizzes_5", "5 quizzes finished") { it.quizzesDone >= 5 },
        Milestone("quizzes_all", "Every quiz finished") { it.quizzesTotal > 0 && it.quizzesDone >= it.quizzesTotal },
        Milestone("listen_1h", "1 hour of listening") { it.listeningSeconds >= 3600L },
        Milestone("listen_10h", "10 hours of listening") { it.listeningSeconds >= 36000L }
    )

    /** What the numbers reach right now. */
    fun reachedNow(s: ProfileStats): List<Milestone> = ALL.filter { it.reachedBy(s) }

    /** Reached now or told about before. A milestone never un-reaches, even if a streak breaks. */
    fun reached(s: ProfileStats, seen: Set<String>): List<Milestone> =
        ALL.filter { it.id in seen || it.reachedBy(s) }

    /** Reached now but never told about: these get celebrated. */
    fun fresh(s: ProfileStats, seen: Set<String>): List<Milestone> =
        reachedNow(s).filter { it.id !in seen }

    /** The first one still ahead, or null when there is none. */
    fun next(reached: List<Milestone>): Milestone? {
        val ids = reached.map { it.id }.toSet()
        return ALL.firstOrNull { it.id !in ids }
    }

    /** The line shown for the row. Plain numbers, not counted up. */
    fun rowLine(reached: List<Milestone>, fresh: List<Milestone>): String {
        val base = "Milestones: ${reached.size} of ${ALL.size}"
        return if (fresh.isEmpty()) base else "$base, ${fresh.size} new"
    }

    /** What is said when the row is opened. */
    fun spoken(reached: List<Milestone>): String {
        val parts = mutableListOf("You have reached ${reached.size} of ${ALL.size} milestones.")
        if (reached.isNotEmpty() && reached.size <= 5) parts += "Reached: ${reached.joinToString(", ") { it.title }}."
        next(reached)?.let { parts += "Next goal: ${it.title}." } ?: run { parts += "You have reached every milestone." }
        return parts.joinToString(" ")
    }

    /** What is said once when new ones are reached. Never a long list. */
    fun announcement(fresh: List<Milestone>): String = when (fresh.size) {
        0 -> ""
        1 -> "New milestone: ${fresh[0].title}."
        2, 3 -> "New milestones: ${fresh.dropLast(1).joinToString(", ") { it.title }} and ${fresh.last().title}."
        else -> "You have reached ${fresh.size} new milestones, including ${fresh.last().title}."
    }
}
