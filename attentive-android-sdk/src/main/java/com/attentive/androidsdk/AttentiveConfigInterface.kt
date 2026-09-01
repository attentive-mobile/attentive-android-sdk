package com.attentive.androidsdk

import android.app.Application

/**
 * Public contract exposed by [AttentiveConfig]. Exists primarily so tests and alternate
 * implementations can substitute for the real config.
 *
 * The domain and identity members live on [AttentiveIdentityProvider] — that narrower
 * contract is what [AttentiveApi] depends on.
 */
interface AttentiveConfigInterface : AttentiveIdentityProvider {
    val mode: AttentiveConfig.Mode
    override var userIdentifiers: UserIdentifiers
    val applicationContext: Application
    var notificationIconId: Int
    var notificationIconBackgroundColorResource: Int
    var logLevel: AttentiveLogLevel?

    fun skipFatigueOnCreatives(): Boolean

    fun identify(clientUserId: String)

    fun changeDomain(domain: String)
}
