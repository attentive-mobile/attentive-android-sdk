package com.attentive.androidsdk.push

import android.content.Context
import com.attentive.androidsdk.PersistentStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class TokenProvider {
    private val _tokenFlow = MutableStateFlow<String?>(null)

    /** The current FCM token; null until one is restored or fetched. */
    internal val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    @Volatile
    private var storage: PersistentStorage? = null

    /** The current FCM token. Saved to the bound storage, if any, when set. */
    internal var token: String?
        get() = _tokenFlow.value
        set(value) {
            _tokenFlow.value = value
            storage?.let { persist(it, value) }
        }

    /**
     * Persists [token] to [storage] and restores the saved token if none is in memory.
     * Pass null to stop persisting.
     *
     * @param storage Where to persist the token, or null to stop.
     */
    internal fun bindStorage(storage: PersistentStorage?) {
        this.storage = storage
        if (storage == null || !_tokenFlow.value.isNullOrBlank()) return
        val persisted = storage.read(PUSH_TOKEN_KEY)
        if (persisted.isNullOrBlank()) return
        Timber.d("Restored persisted FCM push token")
        _tokenFlow.value = persisted
    }

    internal suspend fun getToken(context: Context): Result<TokenFetchResult> {
        return getTokenFromFirebase(context)
    }

    internal suspend fun getTokenFromFirebase(context: Context): Result<TokenFetchResult> {
        Timber.d("getTokenFromFirebase")
        return try {
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val resultToken = task.result
                        token = resultToken
                        continuation.resume(
                            Result.success(
                                TokenFetchResult(
                                    resultToken,
                                    permissionGranted = AttentivePush.getInstance()
                                        .checkPushPermission(context),
                                ),
                            ),
                        )
                    } else {
                        continuation.resume(Result.failure(Exception("Token fetch failed: ${task.exception?.message}")))
                    }
                }
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "Firebase is not initialized. Ensure FirebaseApp.initializeApp(Context) is called before using push features.")
            Result.failure(e)
        }
    }

    /** Saves [value] to [storage], or deletes the saved token when [value] is blank. */
    private fun persist(
        storage: PersistentStorage,
        value: String?,
    ) {
        if (value.isNullOrBlank()) {
            storage.delete(PUSH_TOKEN_KEY)
        } else {
            storage.save(PUSH_TOKEN_KEY, value)
        }
    }

    companion object {
        internal const val PUSH_TOKEN_KEY = "com.attentive.androidsdk.FCM_PUSH_TOKEN"

        private lateinit var INSTANCE: TokenProvider

        @JvmStatic
        fun getInstance(): TokenProvider {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = TokenProvider()
            }
            return INSTANCE
        }
    }
}
