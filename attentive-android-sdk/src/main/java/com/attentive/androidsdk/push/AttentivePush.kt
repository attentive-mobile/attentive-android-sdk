package com.attentive.androidsdk.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.R
import com.attentive.androidsdk.tracking.AppLaunchTracker
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AttentivePush {

    internal suspend fun fetchPushToken(context: Context, requestPermissionIfNotGranted: Boolean): Result<TokenFetchResult> {
        return if (requestPermissionIfNotGranted && !checkPushPermission(context)) {
            requestPushPermission(context)
        } else {
            TokenProvider.getInstance().getToken(context)
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
                        continuation.resume(TokenProvider.getInstance().getTokenFromFirebase(context))
                    }
                } else {
                    continuation.resume(Result.failure(Exception("Permission denied")))
                }
            }
        }
    }

    internal fun sendNotification(remoteMessage: RemoteMessage) {
        Timber.d("sendNotification with data: ${remoteMessage.data} and title ${remoteMessage.notification?.title} and body ${remoteMessage.notification?.body}")
        // Here you would implement the logic to display the notification
        // For example, using NotificationManager to show a notification

        val title = remoteMessage.data.getOrElse("attentive_message_title") {
            null
        }
        val body  = remoteMessage.data.getOrElse("attentive_message_body") { null }

        if(title != null && body != null) {
            //todo nullability check
            val context = AttentiveEventTracker.instance.config?.applicationContext!!
            sendNotification(title, body, remoteMessage.data, context)
        } else {
            Timber.e("Error parsing notification data: $remoteMessage title $title or body: $body is null")
        }
    }

    //TODO make private
    internal fun sendNotification(messageTitle: String, messageBody: String, dataMap: Map<String, String>, context: Context) {
        val channelId = "fcm_default_channel"
        val notificationId = 0

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Launch intent to open the host app's main launcher activity
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppLaunchTracker.LAUNCHED_FROM_NOTIFICATION, true)


            // Add dataMap as extras
            for ((key, value) in dataMap) {
                putExtra(key, value)
            }
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(contentPendingIntent) // Main tap opens app

        // Create channel
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Marketing",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        //Show notification
        notificationManager.notify(notificationId, notificationBuilder.build())
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

