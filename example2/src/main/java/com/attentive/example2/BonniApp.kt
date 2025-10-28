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
        val email = prefs.getString(ATTENTIVE_EMAIL_PREFS, null)
        val phone = prefs.getString(ATTENTIVE_PHONE_PREFS, null)

        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .applicationContext(this)
                .domain(domain)
                .notificationIconId(R.drawable.bonni_logo)
                .notificationIconBackgroundColor(R.color.purple_200)
                .mode(AttentiveConfig.Mode.DEBUG)
                .logLevel(AttentiveLogLevel.VERBOSE).build()


        val userIdentifiers = UserIdentifiers.Builder()
        if(email != null || phone != null){
            email?.let {
                userIdentifiers.withEmail(it)
            }
            phone?.let {
                userIdentifiers.withPhone(it)
            }

            attentiveConfig.userIdentifiers = userIdentifiers.build()
        }


        AttentiveEventTracker.instance.initialize(attentiveConfig)
    }

    companion object {
        private lateinit var appInstance: BonniApp

        const val ATTENTIVE_PREFS = "ATTENTIVE_PREFS"
        const val ATTENTIVE_DOMAIN_PREFS = "ATTENTIVE_DOMAIN_PREFS"

        const val ATTENTIVE_EMAIL_PREFS = "ATTENTIVE_EMAIL_PREFS"
        const val ATTENTIVE_PHONE_PREFS = "ATTENTIVE_PHONE_PREFS"

        const val ATTENTIVE_ENDPOINT_PREFS = "ATTENTIVE_ENDPOINT_PREFS"

        const val ATTENTIVE_ENDPOINT_OLD = "ATTENTIVE_ENDPOINT_OLD"
        const val ATTENTIVE_ENDPOINT_NEW = "ATTENTIVE_ENDPOINT_NEW"

        fun getInstance(): BonniApp {
            return appInstance
        }
    }

}