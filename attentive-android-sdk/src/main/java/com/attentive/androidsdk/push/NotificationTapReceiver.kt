package com.attentive.androidsdk.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.attentive.androidsdk.tracking.AppLaunchTracker

class NotificationTapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLaunchTracker.wasLaunchedFromNotification = true
    }
}