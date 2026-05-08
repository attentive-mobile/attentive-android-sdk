package com.attentive.androidsdk

import android.app.Application

/**
 * Public contract exposed by [AttentiveConfig]. Exists primarily so tests and alternate
 * implementations can substitute for the real config.
 */
interface AttentiveConfigInterface {
    /** The configured SDK runtime mode. */
    val mode: AttentiveConfig.Mode

    /** The configured Attentive domain. */
    val domain: String

    /** The current [UserIdentifiers] attached to this visitor. */
    var userIdentifiers: UserIdentifiers

    /** The host [Application] context. */
    val applicationContext: Application

    /** Drawable resource ID for the notification small icon. `0` means none. */
    var notificationIconId: Int

    /** Color resource ID for the notification icon background. `0` means none. */
    var notificationIconBackgroundColorResource: Int

    /** Current log level, or `null` to use the SDK default. */
    var logLevel: AttentiveLogLevel?

    /** @return `true` if creatives should bypass frequency-capping / fatigue rules. */
    fun skipFatigueOnCreatives(): Boolean

    /**
     * Associates a client-provided user ID with the current visitor.
     *
     * @param clientUserId Non-empty user ID from the host app's system.
     */
    fun identify(clientUserId: String)

    /**
     * Merges the given identifiers into the current visitor and notifies the backend.
     *
     * @param userIdentifiers Identifiers to merge.
     */
    fun identify(userIdentifiers: UserIdentifiers)

    /**
     * Clears local user identifiers. See [AttentiveConfig.clearUser] for deprecation details —
     * prefer [com.attentive.androidsdk.AttentiveSdk.clearUser] for full-logout semantics.
     */
    fun clearUser()

    /**
     * Changes the Attentive domain at runtime.
     *
     * @param domain The new domain.
     */
    fun changeDomain(domain: String)
}
