package com.resonant.app.content

/**
 * Puts the person's chosen material in front of the AI model. The text is cut at a spoken
 * unit so the model never gets half a sentence, and capped so a long file cannot swamp it.
 * Pure Kotlin, tested on the JVM.
 */
object MaterialContext {

    const val MAX_CHARS = 6000

    /** The lesson as plain text, section titles included, ending at a whole unit. */
    fun text(lesson: Lesson, maxChars: Int = MAX_CHARS): String {
        val out = StringBuilder()
        for (section in lesson.sections) {
            val heading = section.title.trim().trimEnd('.', '!', '?') + "."
            val parts = listOf(heading) + section.units.map { it.text.trim() }
            for (part in parts) {
                val add = if (out.isEmpty()) part else " $part"
                if (out.length + add.length > maxChars) return out.toString()
                out.append(add)
            }
        }
        return out.toString()
    }

    /** [base] unchanged when there is no material. */
    fun systemPrompt(base: String, lesson: Lesson?): String {
        if (lesson == null) return base
        return base + " The student is studying their own material, titled \"${lesson.title}\". " +
            "Answer from it when the question is about it, and keep to what it says. " +
            "If it does not cover the question, say so in one short sentence, then help from what you know. " +
            "The material follows. " + text(lesson)
    }
}
