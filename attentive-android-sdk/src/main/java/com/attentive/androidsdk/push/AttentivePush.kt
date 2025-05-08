package com.attentive.androidsdk.push

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AttentivePush {

    internal suspend fun fetchPushToken(context: Context, requestPermissionIfNotGranted: Boolean = false): Result<TokenFetchResult> {
        return if (requestPermissionIfNotGranted && !checkPushPermission(context)) {
            requestPushPermission(context)
        } else {
            getTokenFromFirebase()
        }
    }

    internal fun checkPushPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission is not required for APIs below 33
        }
    }

    private suspend fun requestPushPermission(context: Context): Result<TokenFetchResult> {
        Timber.d("requestPushPermission")
        return suspendCancellableCoroutine { continuation ->
            PermissionRequestActivity.request(context) { isGranted ->
                Timber.d("Permission granted: $isGranted")
                if (isGranted) {
                    CoroutineScope(Dispatchers.Default).launch {
                        continuation.resume(getTokenFromFirebase())
                    }
                } else {
                    continuation.resume(Result.failure(Exception("Permission denied")))
                }
            }
        }
    }

    private suspend fun getTokenFromFirebase(): Result<TokenFetchResult> {
        Timber.d("getTokenFromFirebase")
        return suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Timber.d("Token: $token")
                    continuation.resume(Result.success(TokenFetchResult(token)))
                } else {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Token fetch failed")
                    )
                }
            }
        }
    }

    companion object {
        lateinit var INSTANCE: AttentivePush
        fun getInstance(): AttentivePush {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = AttentivePush()
            }
            return INSTANCE
        }
    }

    class PermissionRequestActivity : AppCompatActivity() {

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        private val activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                callback?.invoke(isGranted)
                finish()
            }

        companion object {
            private var callback: ((Boolean) -> Unit)? = null

            internal fun request(context: Context, callback: (Boolean) -> Unit) {
                this.callback = callback
                val intent = Intent(context, PermissionRequestActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}

