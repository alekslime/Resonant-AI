package com.resonant.app

import android.app.Application
import com.resonant.app.core.ResonantContainer
import com.resonant.app.livekit.LiveKitManager

class ResonantApp : Application() {
    lateinit var container: ResonantContainer
        private set

    lateinit var liveKitManager: LiveKitManager
        private set

    override fun onCreate() {
        super.onCreate()

        container = ResonantContainer(this)
        liveKitManager = LiveKitManager(this)
    }
}
