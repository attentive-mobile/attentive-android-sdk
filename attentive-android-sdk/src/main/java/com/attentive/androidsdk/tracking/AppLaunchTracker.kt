package com.attentive.androidsdk.tracking

import android.app.Activity
import android.app.Application
import android.os.Bundle
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

    init {
        registerAppLaunchTracker()
        registerActivityCallback()
    }

    val launchEvents = mutableListOf<AttentiveApi.LaunchType>()
    var dataMap = mutableMapOf<String, String>()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("App moved to foreground")

        AttentiveEventTracker.instance.config?.applicationContext?.let {
            AttentiveEventTracker.instance.registerPushToken(it)
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
                Timber.d("Activity created: ${activity.localClassName}")
                var wasLaunchedFromNotification = activity.intent?.extras?.run {
                    getBoolean(LAUNCHED_FROM_NOTIFICATION, false)
                } == true

                Timber.d("Launched from notification: $wasLaunchedFromNotification")

                if (wasLaunchedFromNotification) {
                    Timber.d("Launched from notification")
                    launchEvents.add(AttentiveApi.LaunchType.DIRECT_OPEN)
                } else {
                    Timber.d("Launched normally")
                }


            }


            override fun onActivityStarted(activity: Activity) {
                Timber.d("Activity started: ${activity.localClassName}")
                CoroutineScope(Dispatchers.IO).launch {
                    if (launchEvents.contains(AttentiveApi.LaunchType.DIRECT_OPEN)) {
                        activity.intent.extras.run {
                            if (this != null) {
                                for (key in keySet()) {
                                    if(key != LAUNCHED_FROM_NOTIFICATION) {
                                        dataMap[key] = getString(key).toString()
                                    }
                                }
                            }
                        }
                        AttentiveEventTracker.instance.sendAppLaunchEvent(AttentiveApi.LaunchType.DIRECT_OPEN, dataMap)
                    } else {
                        AttentiveEventTracker.instance.sendAppLaunchEvent(AttentiveApi.LaunchType.APP_LAUNCHED)

                    }
                }
            }

            override fun onActivityResumed(activity: Activity) {
                Timber.d("Activity resumed: ${activity.localClassName}")
            }

            override fun onActivityPaused(activity: Activity) {
                Timber.d("Activity paused: ${activity.localClassName}")
            }

            override fun onActivityStopped(activity: Activity) {
                Timber.d("onActivityStopped: ${activity.localClassName}")
                launchEvents.clear()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                Timber.d("onActivitySaveInstanceState: ${activity.localClassName}")
            }

            override fun onActivityDestroyed(activity: Activity) {
                Timber.d("onActivityDestroyed: ${activity.localClassName}")
            }
        })
    }

    companion object {
        val LAUNCHED_FROM_NOTIFICATION = "launched_from_notification"
    }
}