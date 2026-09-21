package com.resonant.app.network

import com.resonant.app.BuildConfig

/**
 * Resonant's Chat feature talks to an Ollama server on your local network —
 * there is no cloud API key anywhere in this app.
 *
 * The DEFAULTS are NOT edited here. They come from `local.properties` in the
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
 *
 * The build-time values can be overridden on the phone, without rebuilding, from
 * Settings -> Debug Mode -> Server setup (say, when a demo is on a different
 * Wi-Fi than the one you built for). [ResonantContainer] loads any saved override
 * at startup; [BASE_URL] and [MODEL] always return the effective value, so
 * nothing that reads them needs to know an override exists.
 */
object OllamaConfig {
    val DEFAULT_BASE_URL: String = BuildConfig.OLLAMA_BASE_URL.trimEnd('/')
    val DEFAULT_MODEL: String = BuildConfig.OLLAMA_MODEL

    @Volatile private var baseUrlOverride: String? = null
    @Volatile private var modelOverride: String? = null

    val BASE_URL: String get() = baseUrlOverride ?: DEFAULT_BASE_URL
    val MODEL: String get() = modelOverride ?: DEFAULT_MODEL

    /** Blank or null clears an override, falling back to the build-time default. */
    fun applyOverrides(baseUrl: String?, model: String?) {
        baseUrlOverride = baseUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
        modelOverride = model?.trim()?.takeIf { it.isNotEmpty() }
    }
}
