package com.attentive.androidsdk.internal.identity

import com.attentive.androidsdk.PersistentStorage

/**
 * One `/user-update` the backend accepted, in full: the domain and visitor it was sent for, the
 * push token it carried, and the contact info attached to that token.
 *
 * Every field is part of the identity of the sync, because changing any one of them means the
 * backend has *not* been told the resulting state:
 * - [domain] — a different Attentive account entirely. The same user must be re-associated after
 *   [com.attentive.androidsdk.AttentiveConfig.changeDomain].
 * - [visitorId] — the association hangs off the visitor. Once the visitor rotates, the backend
 *   has never seen this contact info on the new one.
 * - [pushToken] — a rotated token has to be re-attached or push goes to the old token.
 */
internal data class IdentitySyncRecord(
    val domain: String,
    val visitorId: String,
    val pushToken: String,
    val email: String?,
    val phone: String?,
)

/**
 * What the SDK last successfully told the backend about this device.
 *
 * Local identifiers alone can't answer "does the backend already know this?" — they're rebuilt
 * from nothing on every launch and they're set optimistically, before the request completes. This
 * record is written only after a `/user-update` succeeds, so an unconfirmed or failed sync is
 * retried on the next call instead of being mistaken for a no-op.
 *
 * Persisted so the guard survives a process restart, which is exactly when host apps re-issue
 * the calls this is meant to absorb.
 */
internal class IdentitySyncStore(private val persistentStorage: PersistentStorage) {
    /**
     * True when the last confirmed sync was exactly [candidate], meaning another `/user-update`
     * would tell the backend nothing new.
     *
     * A null [candidate] never matches: the caller couldn't assemble a complete record (no push
     * token or no visitor ID yet), so there is nothing to compare and it should proceed rather
     * than guess.
     */
    fun matches(candidate: IdentitySyncRecord?): Boolean = candidate != null && read() == candidate

    /**
     * The last confirmed sync, or null if none was ever recorded or the record is incomplete.
     */
    fun read(): IdentitySyncRecord? {
        val domain = persistentStorage.read(DOMAIN_KEY) ?: return null
        val visitorId = persistentStorage.read(VISITOR_ID_KEY) ?: return null
        val pushToken = persistentStorage.read(PUSH_TOKEN_KEY) ?: return null
        return IdentitySyncRecord(
            domain = domain,
            visitorId = visitorId,
            pushToken = pushToken,
            email = persistentStorage.read(EMAIL_KEY),
            phone = persistentStorage.read(PHONE_KEY),
        )
    }

    /**
     * Records a `/user-update` the backend accepted. Call only on success.
     */
    fun recordSuccessfulSync(sync: IdentitySyncRecord) {
        persistentStorage.save(DOMAIN_KEY, sync.domain)
        persistentStorage.save(VISITOR_ID_KEY, sync.visitorId)
        persistentStorage.save(PUSH_TOKEN_KEY, sync.pushToken)
        write(EMAIL_KEY, sync.email)
        write(PHONE_KEY, sync.phone)
    }

    private fun write(key: String, value: String?) {
        if (value == null) {
            persistentStorage.delete(key)
        } else {
            persistentStorage.save(key, value)
        }
    }

    companion object {
        internal const val DOMAIN_KEY = "com.attentive.androidsdk.LAST_SYNCED_DOMAIN"
        internal const val VISITOR_ID_KEY = "com.attentive.androidsdk.LAST_SYNCED_VISITOR_ID"
        internal const val PUSH_TOKEN_KEY = "com.attentive.androidsdk.LAST_SYNCED_PUSH_TOKEN"
        internal const val EMAIL_KEY = "com.attentive.androidsdk.LAST_SYNCED_EMAIL"
        internal const val PHONE_KEY = "com.attentive.androidsdk.LAST_SYNCED_PHONE"
    }
}

/**
 * What [com.attentive.androidsdk.AttentiveSdk] should do with an identity call, decided in one
 * locked step so concurrent callers can't each rotate the visitor ID.
 */
internal enum class IdentitySyncDecision {
    /** Local state and the backend already agree — do nothing. */
    SKIP,

    /**
     * Local state is already correct but the backend never confirmed it, so send the
     * `/user-update` again *without* minting a new visitor ID. This is the relaunch and
     * failed-request path: the identity is unchanged, only the sync is outstanding.
     */
    RETRY_WITHOUT_ROTATION,

    /** A genuine identity change — the visitor ID was rotated and the new identifiers installed. */
    ROTATED_AND_REPLACED,
}
