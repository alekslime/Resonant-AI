package com.resonant.app.core

import android.content.Context

/**
 * The only persisted state in Resonant: whether the user has been through the
 * gesture tutorial. Deliberately a plain SharedPreferences wrapper — there is no
 * database here and adding DataStore for a single boolean would be overkill.
 */
class ResonantPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("resonant_prefs", Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
