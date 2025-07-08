package com.attentive.example2

import android.app.Application
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.UserIdentifiers
import timber.log.Timber


class AttentiveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        initAttentiveTracker()
        Timber.plant(Timber.DebugTree())
    }

    private fun initAttentiveTracker() {
        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .context(this)
                .domain("YOUR_ATTENTIVE_DOMAIN") // Replace with your Attentive domain
                .mode(AttentiveConfig.Mode.PRODUCTION)
                .logLevel(AttentiveLogLevel.VERBOSE).build()

        val userIdentifiers =
            UserIdentifiers.Builder().withClientUserId("BonniAndroid").withPhone("+15556667777").withEmail("bonni@bonnibeauty.com")
                .build()
        attentiveConfig.identify(userIdentifiers)

        AttentiveEventTracker.instance.initialize(attentiveConfig)
    }

    companion object {
        private lateinit var appInstance: AttentiveApp
        fun getInstance(): AttentiveApp {
            return appInstance
        }
    }

}