package com.attentive.example2

import android.app.Application
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel

class BonniApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        initAttentiveTracker()
    }

    private fun initAttentiveTracker() {
        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .context(this)
                .domain("games")
                .mode(AttentiveConfig.Mode.DEBUG)
                .logLevel(AttentiveLogLevel.VERBOSE).build()

        AttentiveEventTracker.instance.initialize(attentiveConfig)
    }

    companion object {
        private lateinit var appInstance: BonniApp
        fun getInstance(): BonniApp {
            return appInstance
        }
    }

}