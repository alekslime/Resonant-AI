package com.resonant.app.network

import com.resonant.app.BuildConfig

/**
 * Resonant's Chat feature talks to an Ollama server on your local network —
 * there is no cloud API key anywhere in this app.
 *
 * These values are NOT edited here. They come from `local.properties` in the
 * project root (which is git-ignored, so your LAN address never lands in a
 * commit) and are baked in at build time:
 *
 *     ollama.baseUrl=http://192.168.1.50:11434
 *     ollama.model=llama3.2
 *
 * - baseUrl: the machine running `ollama serve`, reachable from your phone.
 *   The default `10.0.2.2` only works from the Android *emulator* (it's the
 *   emulator's alias for the host's localhost). On a physical device, phone
 *   and server must be on the same Wi-Fi and this must be the server's LAN IP.
 * - model: any model you've already pulled (`ollama pull <name>`).
 *
 * Plain HTTP is only permitted in debug builds (see src/debug/AndroidManifest.xml).
 * A release build needs an https:// URL.
 */
object OllamaConfig {
    val BASE_URL: String = BuildConfig.OLLAMA_BASE_URL.trimEnd('/')
    val MODEL: String = BuildConfig.OLLAMA_MODEL
}
