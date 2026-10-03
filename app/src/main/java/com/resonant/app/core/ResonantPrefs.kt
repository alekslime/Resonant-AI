package com.resonant.app.core

import android.content.Context
import com.resonant.app.audio.AudioManager

/**
 * The only persisted state in Resonant: whether the user has been through the
 * gesture tutorial, their chosen speech speed, whether sound cues are on, and an
 * optional Ollama server address/model that overrides the build-time default. Deliberately a plain
 * SharedPreferences wrapper — there is no database here and adding DataStore for
 * a handful of values would be overkill.
 */
class ResonantPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("resonant_prefs", Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    /** Index into [AudioManager.SPEEDS]. Out-of-range values are clamped by the reader. */
    var speedIndex: Int
        get() = prefs.getInt(KEY_SPEED_INDEX, AudioManager.DEFAULT_SPEED_INDEX)
        set(value) = prefs.edit().putInt(KEY_SPEED_INDEX, value).apply()

    /** Whether navigation/feedback tones play alongside the haptics. On by default. */
    var soundCuesEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_CUES, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_CUES, value).apply()

    /** Null means "use the build-time default". */
    var ollamaBaseUrl: String?
        get() = prefs.getString(KEY_OLLAMA_BASE_URL, null)
        set(value) = putOrRemove(KEY_OLLAMA_BASE_URL, value)

    var ollamaModel: String?
        get() = prefs.getString(KEY_OLLAMA_MODEL, null)
        set(value) = putOrRemove(KEY_OLLAMA_MODEL, value)

    /** First name shown in the Chat greeting ("Hi Marc!"). Null/blank = a generic greeting. */
    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = putOrRemove(KEY_USER_NAME, value?.trim())

    /** Kokoro voice id the PC agent should speak with in Live mode. */
    var liveVoice: String
        get() = prefs.getString(KEY_LIVE_VOICE, null)?.takeIf { v -> LIVE_VOICES.any { it.id == v } } ?: LIVE_VOICES.first().id
        set(value) = prefs.edit().putString(KEY_LIVE_VOICE, value).apply()

    /** Kokoro speaking speed for Live mode, one of [LIVE_SPEEDS]. */
    var liveSpeed: Float
        get() = prefs.getFloat(KEY_LIVE_SPEED, 1.0f).takeIf { it in LIVE_SPEEDS } ?: 1.0f
        set(value) = prefs.edit().putFloat(KEY_LIVE_SPEED, value).apply()

    /** Live mode: false = the mic stays open, true = it is open only while the centre is held. */
    var liveHoldToTalk: Boolean
        get() = prefs.getBoolean(KEY_LIVE_HOLD, false)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_HOLD, value).apply()

    private fun putOrRemove(key: String, value: String?) {
        val editor = prefs.edit()
        if (value.isNullOrBlank()) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    data class LiveVoice(val id: String, val label: String)

    companion object {
        val LIVE_VOICES = listOf(
            LiveVoice("af_heart", "Heart"),
            LiveVoice("af_bella", "Bella"),
            LiveVoice("af_nicole", "Nicole"),
            LiveVoice("af_sarah", "Sarah"),
            LiveVoice("af_sky", "Sky"),
            LiveVoice("am_adam", "Adam"),
            LiveVoice("am_michael", "Michael"),
            LiveVoice("bf_emma", "Emma, British"),
            LiveVoice("bf_isabella", "Isabella, British"),
            LiveVoice("bm_george", "George, British"),
            LiveVoice("bm_lewis", "Lewis, British")
        )
        val LIVE_SPEEDS = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.25f, 1.5f)

        private const val KEY_LIVE_VOICE = "live_voice"
        private const val KEY_LIVE_SPEED = "live_speed"
        private const val KEY_LIVE_HOLD = "live_hold_to_talk"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_SPEED_INDEX = "speed_index"
        const val KEY_SOUND_CUES = "sound_cues"
        const val KEY_OLLAMA_BASE_URL = "ollama_base_url"
        const val KEY_OLLAMA_MODEL = "ollama_model"
        const val KEY_USER_NAME = "user_name"
    }
}
