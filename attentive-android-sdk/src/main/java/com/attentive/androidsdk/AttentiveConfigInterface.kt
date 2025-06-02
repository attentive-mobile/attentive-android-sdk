package com.attentive.androidsdk

import android.app.Application


interface AttentiveConfigInterface {
    val mode: AttentiveConfig.Mode
    val domain: String
    var userIdentifiers: UserIdentifiers
    val applicationContext: Application
    var clientWillHandlePushToken: Boolean

    fun skipFatigueOnCreatives(): Boolean
    fun identify(clientUserId: String)
    fun identify(userIdentifiers: UserIdentifiers)
    fun clearUser()
    fun changeDomain(domain: String)
}
