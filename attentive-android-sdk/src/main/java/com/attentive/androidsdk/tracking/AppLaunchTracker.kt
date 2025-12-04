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
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.androidsdk.internal.util.toJsonEncodedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject
import timber.log.Timber

internal class AppLaunchTracker(
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
            CoroutineScope(Dispatchers.IO).launch {
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
                                    //All the metadata for the notification is packaged in the launch intent
                                    //The attentive backend needs these to be sent back to it when a notification is tapped
                                    //The LAUNCHED_FROM_NOTIFICATION flag is a flag used for internal use only and shouldn't be sent to the backend
                                    if (key != LAUNCHED_FROM_NOTIFICATION) {
                                        dataMap[key] = getString(key).toString()
                                    }
                                }
                            }
                        }

                        jsonEncodeTitleAndBody()

                        AttentiveEventTracker.instance.sendAppLaunchEvent(
                            AttentiveApi.LaunchType.DIRECT_OPEN,
                            dataMap
                        )
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

    fun jsonEncodeTitleAndBody(){
        dataMap[Constants.Companion.KEY_NOTIFICATION_BODY]?.let {
            dataMap[Constants.Companion.KEY_NOTIFICATION_BODY] = it.toJsonEncodedString()
        }

        dataMap[Constants.Companion.KEY_NOTIFICATION_TITLE]?.let {
            dataMap[Constants.Companion.KEY_NOTIFICATION_TITLE] = it.toJsonEncodedString()
        }
    }

    companion object {
        val LAUNCHED_FROM_NOTIFICATION = "launched_from_notification"
    }
}