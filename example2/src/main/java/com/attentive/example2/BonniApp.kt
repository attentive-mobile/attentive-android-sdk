package com.attentive.example2

import android.app.Application
import android.graphics.Color
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.UserIdentifiers
import timber.log.Timber


class BonniApp : Application() {

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
                .applicationContext(this)
                .domain("games")
                .notificationIconId(R.drawable.bonni_logo)
                .notificationIconBackgroundColor(R.color.purple_200)
                .mode(AttentiveConfig.Mode.DEBUG)
                .logLevel(AttentiveLogLevel.VERBOSE).build()

        val userIdentifiers =
            UserIdentifiers.Builder().withClientUserId("BonniAndroid").withPhone("+15556667777").withEmail("bonni@bonnibeauty.com")
                .build()
        attentiveConfig.identify(userIdentifiers)

        AttentiveEventTracker.instance.initialize(attentiveConfig)
    }

    companion object {
        private lateinit var appInstance: BonniApp
        fun getInstance(): BonniApp {
            return appInstance
        }
    }

}