package com.resonant.app.network

/**
 * Resonant's Chat feature talks to an Ollama server on your local network —
 * there is no cloud API key anywhere in this app. Edit these two values to
 * match your setup:
 *
 * - BASE_URL: the machine running `ollama serve`, reachable from your phone.
 *   `10.0.2.2` only works from the Android *emulator* (it's the emulator's
 *   alias for your host machine's localhost). On a physical device, phone
 *   and server must be on the same Wi-Fi, and this must be that machine's
 *   LAN IP, e.g. "http://192.168.1.50:11434".
 * - MODEL: any model you've already pulled (`ollama pull <name>`).
 */
object OllamaConfig {
    const val BASE_URL = "http://192.168.0.7:11434"
    const val MODEL = "llama3.2"
}
