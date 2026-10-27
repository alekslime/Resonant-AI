package com.resonant.app.phoneai

import com.resonant.app.network.ChatMessage

/**
 * The rules for the on-phone AI that need no Android and no model, so the JVM tests can run them:
 * can this phone take it, what to ask of the model, how to resume a half-finished download.
 */
object PhoneAiSupport {

    /** A phone reports a bit under its label: a 4 GB phone shows about 3.7 GB. 3 GB phones fall below this. */
    const val MIN_RAM_BYTES = 2_800_000_000L

    sealed class Verdict {
        data object Ok : Verdict()
        /** [spoken] is what to say to the person. */
        data class No(val spoken: String) : Verdict()
    }

    fun check(abis: List<String>, cpuInfo: String, totalRamBytes: Long): Verdict = when {
        "arm64-v8a" !in abis ->
            Verdict.No("The on-phone AI needs a 64-bit ARM phone, and this one is not.")
        totalRamBytes in 1 until MIN_RAM_BYTES ->
            Verdict.No("This phone has too little memory for the on-phone AI.")
        cpuInfo.isNotBlank() && !hasDotProduct(cpuInfo) ->
            Verdict.No("This phone's processor is too old for the on-phone AI.")
        else -> Verdict.Ok
    }

    /** The "asimddp" flag in /proc/cpuinfo: the ARM instructions the model code is built to use. */
    fun hasDotProduct(cpuInfo: String): Boolean =
        cpuInfo.lineSequence().any { line ->
            line.startsWith("Features") && line.split(' ', '\t', ':').contains("asimddp")
        }

    /** The build's default server is the emulator's alias for the PC, which a real phone cannot reach. */
    fun noServerConfigured(baseUrl: String): Boolean = baseUrl.contains("//10.0.2.2")

    /** How a download carries on from [existingBytes] already saved, given the server's answer. Null: give up. */
    data class ResumePlan(val append: Boolean, val totalBytes: Long)

    fun resumePlan(status: Int, existingBytes: Long, contentLength: Long): ResumePlan? = when (status) {
        200 -> ResumePlan(append = false, totalBytes = contentLength)
        206 -> ResumePlan(append = true, totalBytes = if (contentLength > 0) existingBytes + contentLength else -1)
        else -> null
    }
}

/**
 * Cuts what Chat would send to a big model down to what a small one on a slow processor can read
 * in a few seconds. The start of the system prompt (the instructions) always survives, and so does
 * the newest question; the oldest turns go first.
 */
object PhoneAiPrompt {

    const val SYSTEM_CHARS = 3000
    const val HISTORY_CHARS = 2000
    const val QUESTION_CHARS = 1000
    const val OLD_TURN_CHARS = 400

    /** [history] as Chat builds it: a system message, earlier user/assistant turns, then the new question. */
    fun fit(history: List<ChatMessage>, scale: Double = 1.0): List<ChatMessage> {
        val system = history.firstOrNull { it.role == "system" }
        val rest = history.filter { it.role != "system" }
        if (rest.isEmpty()) return listOfNotNull(system?.cut(SYSTEM_CHARS, scale))

        val question = rest.last().cut(QUESTION_CHARS, scale)

        var budget = (HISTORY_CHARS * scale).toInt()
        val kept = ArrayDeque<ChatMessage>()
        for (message in rest.dropLast(1).asReversed()) {
            val trimmed = message.cut(OLD_TURN_CHARS, scale)
            if (trimmed.content.length > budget) break
            budget -= trimmed.content.length
            kept.addFirst(trimmed)
        }
        // A reply with no question in front of it confuses the chat format.
        while (kept.isNotEmpty() && kept.first().role != "user") kept.removeFirst()

        return listOfNotNull(system?.cut(SYSTEM_CHARS, scale)) + kept + question
    }

    private fun ChatMessage.cut(max: Int, scale: Double) =
        copy(content = cutAtSentence(content, (max * scale).toInt()))

    /** [text] up to [max] characters, ending at a whole sentence when there is one in the second half. */
    fun cutAtSentence(text: String, max: Int): String {
        if (text.length <= max) return text
        val head = text.substring(0, max)
        val stop = head.lastIndexOfAny(charArrayOf('.', '!', '?'))
        if (stop >= max / 2) return head.substring(0, stop + 1)
        val space = head.lastIndexOf(' ')
        return if (space >= max / 2) head.substring(0, space) else head
    }
}
