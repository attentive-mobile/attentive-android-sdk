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

    /**
     * No-op. Always returns `false`. Fatigue rules are no longer skippable from the SDK.
     */
    @Deprecated(
        "Fatigue is no longer skippable from the SDK; this always returns false and will be " +
            "removed in a future major version.",
        level = DeprecationLevel.WARNING,
    )
    fun skipFatigueOnCreatives(): Boolean = false

    fun identify(clientUserId: String)

    fun changeDomain(domain: String)
}
