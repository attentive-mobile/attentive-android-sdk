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
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.R
import com.attentive.androidsdk.tracking.AppLaunchTracker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import timber.log.Timber

class AttentiveFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Timber.d("Refreshed token: $token")
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            AttentiveEventTracker.instance.registerPushToken(this@AttentiveFirebaseMessagingService)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("Message received with data: ${remoteMessage.data} and title ${remoteMessage.notification?.title} and body ${remoteMessage.notification?.body}")


        Timber.d(remoteMessage.data.toString())

        if(AttentiveSdk.getInstance().isAttentiveFirebaseMessage(remoteMessage)) {
            AttentivePush.getInstance().sendNotification(remoteMessage)
        }
    }


}