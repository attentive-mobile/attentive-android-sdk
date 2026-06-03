package com.attentive.androidsdk

import android.app.Application

/**
 * Public contract exposed by [AttentiveConfig]. Exists primarily so tests and alternate
 * implementations can substitute for the real config.
 */
interface AttentiveConfigInterface {
    val mode: AttentiveConfig.Mode
    val domain: String
    var userIdentifiers: UserIdentifiers
    val applicationContext: Application
    var notificationIconId: Int
    var notificationIconBackgroundColorResource: Int
    var logLevel: AttentiveLogLevel?

    fun skipFatigueOnCreatives(): Boolean

    fun identify(clientUserId: String)

    fun identify(userIdentifiers: UserIdentifiers)

    fun clearUser()

    fun changeDomain(domain: String)
}
