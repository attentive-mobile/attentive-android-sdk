package com.attentive.androidsdk.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.attentive.androidsdk.tracking.AppLaunchTracker
import timber.log.Timber

class NotificationTapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("NotificationTapReceiver onReceive called")
        AppLaunchTracker.wasLaunchedFromNotification = true
    }
}