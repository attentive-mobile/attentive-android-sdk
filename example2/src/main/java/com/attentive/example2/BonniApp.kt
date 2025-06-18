package com.attentive.example2

import android.app.Application
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
                .mode(AttentiveConfig.Mode.DEBUG)
                .logLevel(AttentiveLogLevel.VERBOSE).build()

        val userIdentifiers =
            UserIdentifiers.Builder().withClientUserId("BonniAndroid").withPhone("+15556667777").withEmail("bonni@bonnibeauty.com")
                .build()
        attentiveConfig.identify(userIdentifiers)

        AttentiveEventTracker.instance.initialize(attentiveConfig)

//        AttentiveSdk.sendMockNotification("Bonni Beauty", "Welcome to Bonni Beauty! We are excited to have you here. Check out our latest products and offers.", R.drawable.ic_stat_emoji_nature, this)
    }

    companion object {
        private lateinit var appInstance: BonniApp
        fun getInstance(): BonniApp {
            return appInstance
        }
    }

}