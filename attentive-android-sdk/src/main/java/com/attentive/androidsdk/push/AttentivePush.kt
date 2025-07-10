package com.attentive.androidsdk.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.R
import com.attentive.androidsdk.tracking.AppLaunchTracker
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.coroutines.resume
import androidx.core.net.toUri

internal class AttentivePush {


    internal suspend fun fetchPushToken(
        context: Context,
        requestPermissionIfNotGranted: Boolean
    ): Result<TokenFetchResult> {
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
                        continuation.resume(
                            TokenProvider.getInstance().getTokenFromFirebase(context)
                        )
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
        val body = remoteMessage.data.getOrElse("attentive_message_body") { null }

        if (title != null && body != null) {
            val context = AttentiveEventTracker.instance.config?.applicationContext
            context?.let {
                sendNotification(title, body, remoteMessage.data, it)
            }
        } else {
            Timber.e("Error parsing notification data: $remoteMessage title $title or body: $body is null")
        }
    }


    //TODO make private
    internal fun sendNotification(
        messageTitle: String,
        messageBody: String,
        dataMap: Map<String, String>,
        context: Context
    ) {
        val channelId = "fcm_default_channel"
        val notificationId = 47732113
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        var launchIntent = buildLaunchIntent(context, dataMap)

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
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(contentPendingIntent)

        val notificationIconId = AttentiveEventTracker.instance.config?.notificationIconId ?: 0
        if (notificationIconId == 0) {
            notificationBuilder.setSmallIcon(R.drawable.ic_stat_tag_faces)
        } else {
            notificationBuilder.setSmallIcon(notificationIconId)
        }

        val notificationIconBackgroundColorResourceId =
            AttentiveEventTracker.instance.config?.notificationIconBackgroundColorResource ?: 0

        if (notificationIconBackgroundColorResourceId != 0) {
            notificationBuilder
                .setColorized(true)
                .setColor(ContextCompat.getColor(context, notificationIconBackgroundColorResourceId))
        }

        // Create channel
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Marketing",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        //Show notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    @VisibleForTesting
    fun buildLaunchIntent(context: Context, dataMap: Map<String, String>): Intent? {
        val deepLink = dataMap.getOrElse(ATTENTIVE_DEEP_LINK_KEY) { null }
        var launchIntent: Intent? = null
        if (deepLink?.isNotBlank() == true) {
            Timber.d("Building launch intent from deep link: $deepLink")
            launchIntent = Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
                //Only search for matching intent filters in the consuming app
                `package` = context.packageName
            }
        } else {
            Timber.d("Using launcher activity for package: ${context.packageName}")
            launchIntent =
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppLaunchTracker.LAUNCHED_FROM_NOTIFICATION, true)
                }
        }
        return launchIntent
    }


    companion object {
        const val ATTENTIVE_DEEP_LINK_KEY = "attentive_open_action_url"
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

