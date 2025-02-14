package com.attentive.example_kotlin

import android.app.Application
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.UserIdentifiers
import com.example.example_kotlin.R

class ExampleKotlinApp : Application() {
    // The mode in which to run the Attentive Android SDK
    private val mode = AttentiveConfig.Mode.PRODUCTION

    lateinit var attentiveConfig: AttentiveConfig

    override fun onCreate() {
        super.onCreate()


        // Change this to your Attentive Domain to test with your Attentive account
        val attentiveDomain = getString(R.string.default_domain)

        // Initialize the Attentive SDK. This only has to be done once per application lifecycle.
        attentiveConfig = AttentiveConfig.Builder()
                .domain(attentiveDomain)
                .mode(mode)
                .context(this)
                .skipFatigueOnCreatives(true)
                .logLevel(AttentiveLogLevel.LIGHT)
                .build()

        // AttentiveEventTracker's "initialize" must be called before the AttentiveEventTracker can
        // be used to send events. The method "initialize" only needs to be called once.
        AttentiveEventTracker.instance.initialize(attentiveConfig)

        // Register the current user with the Attentive SDK. This should be done as early as possible.
        attentiveConfig.identify(buildUserIdentifiers())
    }

    companion object {
        fun buildUserIdentifiers(): UserIdentifiers {
            // Add all the identifiers that you have for the current user. All identifiers are
            // optional, but the more you add the better the Attentive SDK will function.
            return UserIdentifiers.Builder()
                .withPhone("+15556667777")
                .withEmail("some_email@gmailfake.com")
                .withKlaviyoId("userKlaviyoId")
                .withShopifyId("userShopifyId")
                .withClientUserId("userClientUserId")
                .withCustomIdentifiers(mapOf(Pair("customIdentifierKey", "customIdentifierValue")))
                .build()
        }
    }
}
