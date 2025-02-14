package com.attentive.androidsdk

interface AttentiveConfigInterface {
    val mode: AttentiveConfig.Mode
    val domain: String
    var userIdentifiers: UserIdentifiers

    fun skipFatigueOnCreatives(): Boolean
    fun identify(clientUserId: String)
    fun identify(userIdentifiers: UserIdentifiers)
    fun clearUser()
    fun changeDomain(domain: String)
}
