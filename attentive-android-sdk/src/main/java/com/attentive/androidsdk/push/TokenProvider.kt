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
        Timber.d("getToken")
        var clientWillHandlePushToken = false
        AttentiveEventTracker.instance.config?.clientWillHandlePushToken?.let {
            clientWillHandlePushToken = it
        }

        if (clientWillHandlePushToken.not()) {
            return getTokenFromFirebase(context)
        } else {
            if (token == null) {
                Timber.e("AttentiveConfig is configured for the host app to provide the push token. Please set the token manually.")
                return Result.failure(Exception("Token not set and fetchFromFirebase is false"))
            } else {
               return Result.success(TokenFetchResult(token!!, permissionGranted = AttentivePush.getInstance().checkPushPermission(context)))
            }
        }
    }

    internal suspend fun getTokenFromFirebase(context: Context): Result<TokenFetchResult> {
        Timber.d("getTokenFromFirebase")
        return suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Timber.d("Token: $token")
                    continuation.resume(
                        Result.success(
                            TokenFetchResult(
                                token,
                                permissionGranted = AttentivePush.getInstance().checkPushPermission(context)
                            )
                        )
                    )
                } else {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Token fetch failed")
                    )
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