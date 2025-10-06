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
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.R
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.androidsdk.tracking.AppLaunchTracker
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okio.ByteString.Companion.decodeBase64
import timber.log.Timber
import java.net.URL
import kotlin.coroutines.resume

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

        val title = remoteMessage.data.getOrElse(Constants.Companion.KEY_NOTIFICATION_TITLE) {
            null
        }
        val body = remoteMessage.data.getOrElse(Constants.Companion.KEY_NOTIFICATION_BODY) { null }

        val imageUrl = remoteMessage.getImageUrl()

        if (title != null && body != null) {
            val context = AttentiveEventTracker.instance.config.applicationContext
            context?.let {
                sendNotification(title, body, remoteMessage.data, imageUrl, it)
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
        imageUrl: String?,
        context: Context
    ) {
        val channelId = "fcm_default_channel"
        val notificationId = System.currentTimeMillis().toInt()
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

        val notificationIconId = AttentiveEventTracker.instance.config.notificationIconId ?: 0
        if (notificationIconId == 0) {
            notificationBuilder.setSmallIcon(R.drawable.ic_stat_tag_faces)
        } else {
            notificationBuilder.setSmallIcon(notificationIconId)
        }

        val notificationIconBackgroundColorResourceId =
            AttentiveEventTracker.instance.config.notificationIconBackgroundColorResource ?: 0

        if (notificationIconBackgroundColorResourceId != 0) {
            notificationBuilder
                .setColorized(true)
                .setColor(ContextCompat.getColor(context, notificationIconBackgroundColorResourceId))
        }

        if (imageUrl != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = imageUrl.getBitmapFromUrl()
                if (bitmap != null) {
                    notificationBuilder.setStyle(
                        NotificationCompat.BigPictureStyle().bigPicture(bitmap)
                    )
                }
                withContext(Dispatchers.Main) {
                    showNotification(context, channelId, notificationId, notificationBuilder)
                }
            }
        } else {
            showNotification(context, channelId, notificationId, notificationBuilder)
        }
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        notificationBuilder: NotificationCompat.Builder
    ) {
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

private fun RemoteMessage.getImageUrl() : String? {
    data.getOrElse("attentiveData") { null }?.let {
        val decoded = it.decodeBase64()?.utf8()
        decoded?.let {
            val attentiveDataMap = Json.parseToJsonElement(decoded) as JsonObject
            return attentiveDataMap.get("attentive_image_url").toString().trim('"')
        }
    }

    return null
}




private suspend fun String.getBitmapFromUrl(): android.graphics.Bitmap? {
    return try {
        withContext(Dispatchers.IO) {
            val connection = URL(this@getBitmapFromUrl).openConnection() as java.net.HttpURLConnection
            connection.run {
                doInput = true
                connect()
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error loading notification image")
        null
    }
}

