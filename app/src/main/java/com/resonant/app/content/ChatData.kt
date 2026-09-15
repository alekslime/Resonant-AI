package com.resonant.app.content

/**
 * Chat now talks to a real model (see network/OllamaClient.kt) instead of
 * playing a fixed script, so there's no hardcoded conversation left here —
 * just the framing the model needs to be a good voice assistant, and the
 * greeting spoken once when the screen opens.
 */
object ChatData {

    const val title = "Ask Resonant"

    const val systemPrompt =
        "You are a helpful voice assistant embedded in an accessibility app for " +
        "blind and low-vision users. Every reply you give is read aloud through " +
        "text-to-speech, never shown as text the user can re-read. Keep answers " +
        "short — two to four plain spoken sentences. Never use markdown, bullet " +
        "points, headers, or symbols that don't make sense when spoken."

    const val greeting = "Ask Resonant. Tap the center of the screen to ask a question."
}
