package com.attentive.bonni

import android.app.Application
import android.graphics.Color
import androidx.compose.ui.Modifier
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.androidsdk.internal.network.ApiVersion
import com.attentive.bonni.database.AppDatabase
import timber.log.Timber


class BonniApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        initAttentiveTracker()
        // Timber tree is planted by AttentiveSdk based on logLevel config
        AppDatabase.getInstance().initWithMockProducts()
    }

    private fun initAttentiveTracker() {
        val prefs =  getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        val domain = prefs.getString(ATTENTIVE_DOMAIN_PREFS,"vs")!!
        val email = prefs.getString(ATTENTIVE_EMAIL_PREFS, null)
        val phone = prefs.getString(ATTENTIVE_PHONE_PREFS, null)
        val apiVersion = try {
            ApiVersion.valueOf(prefs.getString(ATTENTIVE_ENDPOINT_PREFS, null) ?: "OLD")
        } catch (e: IllegalArgumentException) {
            ApiVersion.OLD
        }

        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .applicationContext(this)
                .domain(domain)
                .notificationIconId(R.drawable.bonni_logo)
                .notificationIconBackgroundColor(R.color.purple_200)
                .mode(AttentiveConfig.Mode.PRODUCTION)
                .logLevel(AttentiveLogLevel.VERBOSE)
                .skipFatigueOnCreatives(true)
                .apiVersion(apiVersion)
                .build()


        AttentiveSdk.initialize(attentiveConfig)

        // Restore user identifiers if they exist (using identify to preserve visitorId)
        if(email != null || phone != null){
            val userIdentifiers = UserIdentifiers.Builder()
            email?.let {
                userIdentifiers.withEmail(it)
            }
            phone?.let {
                userIdentifiers.withPhone(it)
            }

            attentiveConfig.identify(userIdentifiers.build())
        }
    }

    companion object {
        private lateinit var appInstance: BonniApp

        const val ATTENTIVE_PREFS = "ATTENTIVE_PREFS"
        const val ATTENTIVE_DOMAIN_PREFS = "ATTENTIVE_DOMAIN_PREFS"

        const val ATTENTIVE_EMAIL_PREFS = "ATTENTIVE_EMAIL_PREFS"
        const val ATTENTIVE_PHONE_PREFS = "ATTENTIVE_PHONE_PREFS"

        const val ATTENTIVE_ENDPOINT_PREFS = "ATTENTIVE_ENDPOINT_PREFS"

        fun getInstance(): BonniApp {
            return appInstance
        }
    }

}