package com.attentive.androidsdk

import android.content.Context
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenProvider
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AttentiveSdk {

    fun setAndRegisterPushToken(token: String) {
        TokenProvider.getInstance().token = token
        AttentiveEventTracker.instance.config?.applicationContext.let {
            Timber.d("Setting push token: $token")
            CoroutineScope(Dispatchers.IO).launch {
                AttentiveEventTracker.instance.registerPushToken(context = it as Context)
            }
        }
    }

    fun isAttentiveFirebaseMessage(remoteMessage: RemoteMessage): Boolean{
        Timber.d("Checking if message is from Attentive - data: ${remoteMessage.data} and title ${remoteMessage.notification?.title} and body ${remoteMessage.notification?.body}")
        // Check two key sas a fallback in case one gets changed on our backend
        val isAttentiveMessage =  remoteMessage.data.containsKey("attentive_message_title") || remoteMessage.data.containsKey("attentiveData")
        Timber.d("isAttentiveMessage: $isAttentiveMessage")
        return isAttentiveMessage
    }

    fun sendNotification(remoteMessage: RemoteMessage){
        AttentivePush.getInstance().sendNotification(remoteMessage)
    }

    companion object{
        lateinit var INSTANCE: AttentiveSdk

        @JvmStatic
        fun getInstance(): AttentiveSdk {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = AttentiveSdk()
            }
            return INSTANCE
        }
    }
}