package com.attentive.androidsdk.internal.identity

import com.attentive.androidsdk.PersistentStorage

/**
 * What the SDK last successfully told the backend about this device: the push token it was
 * carrying and the contact info attached to it.
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
     * True when the last confirmed sync carried exactly this push token and contact info, meaning
     * another `/user-update` would tell the backend nothing new.
     *
     * A null [pushToken] never matches: without a token there is nothing to compare the record
     * against, so the caller should proceed rather than guess.
     */
    fun matchesLastSync(email: String?, phone: String?, pushToken: String?): Boolean {
        if (pushToken == null) {
            return false
        }
        return persistentStorage.read(PUSH_TOKEN_KEY) == pushToken &&
            persistentStorage.read(EMAIL_KEY) == email &&
            persistentStorage.read(PHONE_KEY) == phone
    }

    /**
     * Records a `/user-update` the backend accepted. Call only on success.
     */
    fun recordSuccessfulSync(email: String?, phone: String?, pushToken: String) {
        persistentStorage.save(PUSH_TOKEN_KEY, pushToken)
        write(EMAIL_KEY, email)
        write(PHONE_KEY, phone)
    }

    private fun write(key: String, value: String?) {
        if (value == null) {
            persistentStorage.delete(key)
        } else {
            persistentStorage.save(key, value)
        }
    }

    companion object {
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
