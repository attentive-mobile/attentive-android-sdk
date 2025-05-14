package com.attentive.androidsdk.tracking

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.AttentiveEventTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AppLaunchTracker(
    internal val application: Application,
    internal val lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle
) : DefaultLifecycleObserver {

    var isFirstLaunch = true


    init {
        registerAppLaunchTracker()
        registerActivityCallback()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("App moved to foreground")
//        owner.lifecycle.currentState.isAtLeast(Lifecycle.State.)

        CoroutineScope(Dispatchers.IO).launch {
            if (AppLaunchTracker.wasLaunchedFromNotification) {
                Timber.d("Launched from notification")
                // Reset flag after handling
                AppLaunchTracker.wasLaunchedFromNotification = false
                AttentiveEventTracker.instance.sendAppLaunchEvent(AttentiveApi.LaunchType.DIRECT_OPEN)
            } else {
                Timber.d("Launched normally")
                AttentiveEventTracker.instance.sendAppLaunchEvent(AttentiveApi.LaunchType.APP_LAUNCHED)
            }

            AttentiveEventTracker.instance.config?.applicationContext?.let {
                AttentiveEventTracker.instance.registerPushToken(it)
            }
        }
    }
    
    fun registerAppLaunchTracker() {
        lifecycle.addObserver(this)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Timber.d("App was already in foreground at registration")
            onStart(ProcessLifecycleOwner.get())
        }
    }

    private fun registerActivityCallback() {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

                wasLaunchedFromNotification = false

                activity.intent?.extras?.getBoolean("sdk_from_notification", false).let {
                    wasLaunchedFromNotification = true
                }

                activity.intent?.extras?.keySet()?.contains("send_id").let {
                    wasLaunchedFromNotification = true
                }


                Timber.d("Launched from notification: $wasLaunchedFromNotification")

                // You can dispatch this to listeners or store it
                //  }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        var wasLaunchedFromNotification = false
    }
}