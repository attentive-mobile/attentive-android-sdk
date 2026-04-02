package com.attentive.androidsdk.push

import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveSdk
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AttentiveFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Timber.i("Refreshed token: $token")
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            AttentiveEventTracker.instance.registerPushToken(this@AttentiveFirebaseMessagingService)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.i(
            "Message received with data: ${remoteMessage.data} and title ${remoteMessage.notification?.title} and body ${remoteMessage.notification?.body}",
        )

        if (AttentiveSdk.isAttentiveFirebaseMessage(remoteMessage)) {
            AttentiveSdk.sendNotification(remoteMessage)
        }
    }
}
