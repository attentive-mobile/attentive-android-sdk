package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenFetchResult
import com.attentive.androidsdk.push.TokenProvider
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import timber.log.Timber

object AttentiveSdk {

    /**
     * Determines whether the given Firebase RemoteMessage is from Attentive.
     */
    fun isAttentiveFirebaseMessage(remoteMessage: RemoteMessage): Boolean {
        Timber.d(
            "%s%s", "Checking if message is from Attentive - data: ${remoteMessage.data}, ", "title: ${remoteMessage.notification?.title}, body: ${remoteMessage.notification?.body}"
        )

        val isAttentiveMessage = remoteMessage.data.containsKey("attentive_message_title") ||
                remoteMessage.data.containsKey("attentiveData")

        Timber.d("isAttentiveMessage: $isAttentiveMessage")
        return isAttentiveMessage
    }

    /**
     * Sets the push token and registers it using the SDK.
     */
//    fun setAndRegisterPushToken(token: String) {
//        TokenProvider.getInstance().token = token
//        val context = AttentiveEventTracker.instance.config?.applicationContext
//
//        if (context != null) {
//            Timber.d("Setting push token: $token")
//            CoroutineScope(Dispatchers.IO).launch {
//                AttentiveEventTracker.instance.registerPushToken(context)
//            }
//        } else {
//            Timber.w("Unable to register push token - context is null")
//        }
//    }

    /**
     * Forwards a push message to the SDK to display the notification.
     */
    fun sendNotification(remoteMessage: RemoteMessage, notificationIconId: Int = 0) {
        AttentivePush.getInstance().sendNotification(remoteMessage, notificationIconId)
    }

    fun sendMockNotification(
        title: String,
        body: String,
        dataMap: Map<String, String>,
        application: Application
    ) {
        AttentivePush.getInstance().sendNotification(messageTitle = title, messageBody = body, dataMap = dataMap, context = application)
    }

    /**
     * Fetches the push token from Firebase, requesting permission if needed.
     */
    suspend fun getPushToken(
        application: Application,
        requestPermission: Boolean
    ): Result<TokenFetchResult> {
        return AttentivePush.getInstance().fetchPushToken(application, requestPermission)
    }

    fun isPushPermissionGranted(context: Context): Boolean {
        return AttentivePush.getInstance().checkPushPermission(context)
    }
}