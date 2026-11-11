package com.attentive.androidsdk.push

import android.content.Context
import com.attentive.androidsdk.ClassFactory
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class TokenProvider {
    private val _tokenFlow = MutableStateFlow<String?>(null)

    /**
     * Emits the FCM token as it becomes known: null on a cold start, then non-null once
     * [loadPersistedToken] hydrates it from disk or a Firebase fetch returns.
     *
     * Callers that must not run token-less — the inbox, which the backend rejects with a 401
     * when `push_token` is absent — collect this rather than reading [token] once and giving up.
     */
    internal val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    internal var token: String?
        get() = _tokenFlow.value
        set(value) {
            _tokenFlow.value = value
        }

    /**
     * Seeds [token] from the last token written to disk, so a warm launch has one before
     * Firebase answers. The in-memory value stays the fast path: an existing one is never
     * replaced by the persisted copy, which may be staler.
     */
    internal fun loadPersistedToken(context: Context) {
        if (!_tokenFlow.value.isNullOrBlank()) return
        val persisted = ClassFactory.buildPersistentStorage(context).read(PUSH_TOKEN_KEY)
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
                        persistToken(context, resultToken)
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

    /**
     * Mirrors the freshly fetched token to disk so the next cold start can read it back
     * immediately instead of racing Firebase again.
     */
    private fun persistToken(
        context: Context,
        value: String?,
    ) {
        val storage = ClassFactory.buildPersistentStorage(context)
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
