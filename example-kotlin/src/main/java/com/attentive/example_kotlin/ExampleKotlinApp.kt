package com.attentive.example_kotlin

import android.app.Application
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.UserIdentifiers

class ExampleKotlinApp : Application() {
    lateinit var attentiveConfig: AttentiveConfig

    companion object {
        fun buildUserIdentifiers(): UserIdentifiers {
            // Add all the identifiers that you have for the current user
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

    override fun onCreate() {
        super.onCreate()
        attentiveConfig = AttentiveConfig("YOUR_ATTENTIVE_DOMAIN", AttentiveConfig.Mode.PRODUCTION, this)
        // "initialize" must be called before any other methods on the AttentiveEventTracker instance
        AttentiveEventTracker.getInstance().initialize(attentiveConfig)

        // Register the current user with the Attentive SDK. This should be done as early as possible.
        attentiveConfig.identify(buildUserIdentifiers())
    }
}
