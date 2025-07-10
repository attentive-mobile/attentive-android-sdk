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
import org.jetbrains.annotations.Nullable
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
     * Forwards a push message to the SDK to display the notification.
     */
    fun sendNotification(remoteMessage: RemoteMessage) {
        AttentivePush.getInstance().sendNotification(remoteMessage)
    }

    @VisibleForTesting
    fun sendMockNotification(
        title: String,
        body: String,
        dataMap: Map<String, String>,
        notificationIconId: Int = 0,
        application: Application
    ) {
        AttentivePush.getInstance().sendNotification(messageTitle = title, messageBody = body, dataMap = dataMap, notificationIconId = notificationIconId, context = application)
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

    /***
     * Does the same as [getPushToken] but uses a callback instead of coroutines for Java interop.
     */
    @JvmStatic
    fun getPushTokenWithCallback(application: Application, requestPermission: Boolean, callback: PushTokenCallback) {
        Timber.d("Synchronously fetching push token with requestPermission: $requestPermission")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = AttentivePush.getInstance().fetchPushToken(application, requestPermission)
                result.getOrNull()?.let {
                    Timber.d("Push token fetched successfully: ${it.token}")
                    callback.onSuccess(it)
                } ?: run {
                    Timber.e("Push token fetch result is null")
                    callback.onFailure(Exception("Push token fetch result is null"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch push token")
                callback.onFailure(e)
            }
        }
    }

    interface PushTokenCallback {
        fun onSuccess(result: TokenFetchResult)
        fun onFailure(exception: Exception)
    }

    fun isPushPermissionGranted(context: Context): Boolean {
        return AttentivePush.getInstance().checkPushPermission(context)
    }
}