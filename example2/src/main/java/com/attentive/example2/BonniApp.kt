package com.attentive.example2

import android.app.Application
import android.graphics.Color
import androidx.compose.ui.Modifier
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.example2.database.AppDatabase
import timber.log.Timber


class BonniApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        initAttentiveTracker()
        Timber.plant(Timber.DebugTree())
        AppDatabase.getInstance().initWithMockProducts()
    }

    private fun initAttentiveTracker() {
        val prefs =  getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        val domain = prefs.getString(ATTENTIVE_DOMAIN_PREFS,"games")!!
        val email = prefs.getString(ATTENTIVE_EMAIL_PREFS, "bonni@bonnibeauty.com")!!

        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .applicationContext(this)
                .domain(domain)
                .notificationIconId(R.drawable.bonni_logo)
                .notificationIconBackgroundColor(R.color.purple_200)
                .mode(AttentiveConfig.Mode.DEBUG)
                .logLevel(AttentiveLogLevel.VERBOSE).build()


        val userIdentifiers =
            UserIdentifiers.Builder().withClientUserId("BonniAndroid").withPhone("+15556667777").withEmail(email)
                .build()
        attentiveConfig.identify(userIdentifiers)

        AttentiveEventTracker.instance.initialize(attentiveConfig)
    }

    companion object {
        private lateinit var appInstance: BonniApp

        const val ATTENTIVE_PREFS = "ATTENTIVE_PREFS"
        const val ATTENTIVE_DOMAIN_PREFS = "ATTENTIVE_DOMAIN_PREFS"

        const val ATTENTIVE_EMAIL_PREFS = "ATTENTIVE_EMAIL_PREFS"
        fun getInstance(): BonniApp {
            return appInstance
        }
    }

}