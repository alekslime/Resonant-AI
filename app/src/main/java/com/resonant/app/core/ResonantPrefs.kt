package com.resonant.app.core

import android.content.Context
import com.resonant.app.audio.AudioManager

/**
 * The only persisted state in Resonant: whether the user has been through the
 * gesture tutorial, their chosen speech speed, and an optional Ollama server
 * address/model that overrides the build-time default. Deliberately a plain
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

    /** Null means "use the build-time default". */
    var ollamaBaseUrl: String?
        get() = prefs.getString(KEY_OLLAMA_BASE_URL, null)
        set(value) = putOrRemove(KEY_OLLAMA_BASE_URL, value)

    var ollamaModel: String?
        get() = prefs.getString(KEY_OLLAMA_MODEL, null)
        set(value) = putOrRemove(KEY_OLLAMA_MODEL, value)

    private fun putOrRemove(key: String, value: String?) {
        val editor = prefs.edit()
        if (value.isNullOrBlank()) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_SPEED_INDEX = "speed_index"
        const val KEY_OLLAMA_BASE_URL = "ollama_base_url"
        const val KEY_OLLAMA_MODEL = "ollama_model"
    }
}
