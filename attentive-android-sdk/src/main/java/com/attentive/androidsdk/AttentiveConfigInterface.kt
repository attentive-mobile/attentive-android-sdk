package com.attentive.androidsdk

import android.app.Application
import androidx.annotation.ColorRes


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
