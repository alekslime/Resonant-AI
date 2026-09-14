package com.resonant.app

import android.app.Application
import com.resonant.app.core.ResonantContainer

class ResonantApp : Application() {
    lateinit var container: ResonantContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = ResonantContainer(this)
    }
}
