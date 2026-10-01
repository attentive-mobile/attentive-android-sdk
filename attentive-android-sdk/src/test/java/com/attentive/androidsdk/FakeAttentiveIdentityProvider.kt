package com.attentive.androidsdk

/**
 * Stand-in for [AttentiveConfig] when exercising [AttentiveApi] on its own.
 *
 * Before [AttentiveIdentityProvider] existed, [AttentiveApi] read this state off
 * `AttentiveEventTracker.instance.config`, so any test touching those paths had to
 * initialize the tracker singleton first. Tests now hand one of these to the constructor.
 */
internal class FakeAttentiveIdentityProvider(
    override var domain: String = "games",
    override var userIdentifiers: UserIdentifiers =
        UserIdentifiers.Builder().withVisitorId("someVisitorId").build(),
) : AttentiveIdentityProvider {
    /** Identifiers passed to [identify], in call order. */
    val identifyCalls = mutableListOf<UserIdentifiers>()

    var clearUserCallCount = 0
        private set

    override fun identify(userIdentifiers: UserIdentifiers) {
        identifyCalls += userIdentifiers
        this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers)
    }

    override fun clearUser() {
        clearUserCallCount++
    }
}
