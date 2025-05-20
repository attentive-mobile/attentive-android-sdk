package com.attentive.androidsdk.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.internal.notify
import timber.log.Timber

class AttentiveFirebaseMessagingService : FirebaseMessagingService() {
    val REQUEST_CODE = 4773713
    override fun onNewToken(token: String) {
        Timber.d("Refreshed token: $token")
        super.onNewToken(token)
        AttentiveEventTracker.instance.registerPushToken(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("Message received with data: ${remoteMessage.data} and title ${remoteMessage.notification?.title} and body ${remoteMessage.notification?.body}")


        Timber.d(remoteMessage.data.toString())

        val title = remoteMessage.data.getValue("message_title")
        val body  = remoteMessage.data.getValue("message_body")
        val dataMap = mutableMapOf<String, String>()

        sendNotification(title, body)
    }


    //TODO make private
    fun sendNotification(messageTitle: String, messageBody: String) {
        val channelId = "fcm_default_channel"
        val notificationId = 0

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Launch intent to open the host app's main launcher activity
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sdk_from_notification", true)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(contentPendingIntent) // Main tap opens app

        // Create channel
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        //Show notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}