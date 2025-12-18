package com.attentive.androidsdk.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TokenProvider {
    internal var token: String? = null

    internal suspend fun getToken(context: Context): Result<TokenFetchResult> {
        return getTokenFromFirebase(context)
    }

    internal suspend fun getTokenFromFirebase(context: Context): Result<TokenFetchResult> {
        Timber.d("getTokenFromFirebase")
        return suspendCancellableCoroutine { continuation ->
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
                    continuation.resume (Result.failure(Exception("Token fetch failed: ${task.exception?.message}")))
                }
            }
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