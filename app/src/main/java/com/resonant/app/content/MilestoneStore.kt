package com.resonant.app.content

import android.content.Context

/**
 * Which milestones the person has already been told about, so each is celebrated once. Its own tiny file, so
 * nothing else that is stored can be touched by it.
 */
class MilestoneStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("resonant_milestones", Context.MODE_PRIVATE)

    fun seen(): Set<String> = prefs.getStringSet(KEY_SEEN, emptySet())?.toSet() ?: emptySet()

    /** Remembers [ids] as told. Never forgets one it already knows. */
    fun markSeen(ids: Collection<String>) {
        prefs.edit().putStringSet(KEY_SEEN, seen() + ids).apply()
    }

    private companion object {
        const val KEY_SEEN = "seen"
    }
}
