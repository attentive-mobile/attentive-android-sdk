package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import com.attentive.androidsdk.AttentiveSdk.getPushToken
import com.attentive.androidsdk.internal.util.isPhoneNumber
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenFetchResult
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import timber.log.Timber

object AttentiveSdk {

    /**
     * Determines whether the given Firebase RemoteMessage is from Attentive.
     */
    fun isAttentiveFirebaseMessage(remoteMessage: RemoteMessage): Boolean {
        Timber.d(
            "%s%s",
            "Checking if message is from Attentive - data: ${remoteMessage.data}, ",
            "title: ${remoteMessage.notification?.title}, body: ${remoteMessage.notification?.body}"
        )

        val isAttentiveMessage = remoteMessage.data.containsKey("attentive_message_title") ||
                remoteMessage.data.containsKey("attentiveData")

        Timber.d("isAttentiveMessage: $isAttentiveMessage")
        return isAttentiveMessage
    }

    suspend fun optUserIntoMarketingSubscription(
        email: String = "",
        phoneNumber: String = ""
    ) {
            if (phoneNumber.isNotBlank() && phoneNumber.isPhoneNumber().not()) {
                Timber.e("Invalid phone number: $phoneNumber")
            }

        AttentiveEventTracker.instance.optIn(email, phoneNumber)
    }

    suspend fun optUserOutOfMarketingSubscription(
        email: String = "",
        phoneNumber: String = ""
    ) {

            if (phoneNumber.isNotBlank() && phoneNumber.isPhoneNumber().not()) {
                Timber.e("Invalid phone number: $phoneNumber")
            }
        AttentiveEventTracker.instance.optOut(email, phoneNumber)
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
        AttentivePush.getInstance().sendNotification(
            messageTitle = title,
            messageBody = body,
            dataMap = dataMap,
            context = application,
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/Cat_November_2010-1a.jpg/960px-Cat_November_2010-1a.jpg"
        )
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
    fun getPushTokenWithCallback(
        application: Application,
        requestPermission: Boolean,
        callback: PushTokenCallback
    ) {
        Timber.d("Synchronously fetching push token with requestPermission: $requestPermission")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result =
                    AttentivePush.getInstance().fetchPushToken(application, requestPermission)
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

    fun updateUser(email: String? = null, phoneNumber: String? = null){
        if(email.isNullOrEmpty() && phoneNumber.isNullOrEmpty()){
            Timber.e("Both email and phone number are empty or null. At least one must be provided to update the user.")
            return
        }

        var number = phoneNumber
        phoneNumber?.let {
            if (it.isPhoneNumber().not()) {
                Timber.e("Invalid phone number: $phoneNumber")
                number = null
            }
        }

        val domain = AttentiveEventTracker.instance.config.domain
        CoroutineScope(Dispatchers.IO).launch {
            AttentiveEventTracker.instance.config.attentiveApi.sendUserUpdate(domain, email, number)
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