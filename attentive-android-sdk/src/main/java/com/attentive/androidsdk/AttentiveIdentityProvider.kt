package com.attentive.androidsdk

/**
 * The domain and identity state that [AttentiveApi] needs in order to build requests.
 *
 * Extracted so [AttentiveApi] can be handed this state at construction rather than reaching
 * back through `AttentiveEventTracker.instance.config`. That static hop made [AttentiveApi]
 * and [AttentiveEventTracker] mutually dependent and left [AttentiveApi] impossible to
 * exercise without initializing the tracker singleton first.
 *
 * [AttentiveConfig] is the production implementation — it already holds all of this state.
 */
interface AttentiveIdentityProvider {
    /**
     * The Attentive domain requests are sent for. Read on each use so runtime changes made
     * through [AttentiveConfigInterface.changeDomain] are picked up.
     */
    val domain: String

    /** Identifiers for the current visitor. */
    val userIdentifiers: UserIdentifiers

    /** Merges [userIdentifiers] into the current visitor and notifies the backend. */
    fun identify(userIdentifiers: UserIdentifiers)

    /** Clears local identifiers and generates a new visitor ID. */
    fun clearUser()
}
