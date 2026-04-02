package com.attentive.androidsdk.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class TokenProvider {
    internal var token: String? = null

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
                        token = task.result
                        continuation.resume(
                            Result.success(
                                TokenFetchResult(
                                    resultToken,
                                    permissionGranted = AttentivePush.getInstance()
                                        .checkPushPermission(context)
                                )
                            )
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

    companion object {
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
