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
    private var hasSentLaunchEvent = false


    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Timber.d("onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("onStart")
        AttentiveEventTracker.instance.config?.applicationContext?.let {
            CoroutineScope(Dispatchers.IO).launch {
                AttentiveEventTracker.instance.registerPushToken(it)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Timber.d("onResume")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        hasSentLaunchEvent = false
    }

    fun registerAppLaunchTracker() {
        Timber.d("Adding lifecycle observer")
        lifecycle.addObserver(this)
        Timber.d("Current state: ${lifecycle.currentState}.")

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            onStart(ProcessLifecycleOwner.get())
        }
    }

    private fun registerActivityCallback() {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
                Timber.d("onActivityStarted")
            }


            override fun onActivityStopped(activity: Activity) {
                launchEvents.clear()
            }

            override fun onActivityResumed(activity: Activity) {
                if (hasSentLaunchEvent) return
                hasSentLaunchEvent = true

                Timber.d("onActivityResumed — intent action: ${activity.intent?.action}, extras: ${activity.intent?.extras?.keySet()?.joinToString()}, data: ${activity.intent?.data}")

                // Check in onActivityResumed (not onActivityStarted) because onResume always fires
                // after onNewIntent. This ensures activity.intent is up to date for singleTop/singleTask
                // activities that call setIntent(intent) in onNewIntent().
                val wasLaunchedFromNotification = activity.intent?.extras?.run {
                    getBoolean(LAUNCHED_FROM_NOTIFICATION, false)
                } == true

                if (wasLaunchedFromNotification) {
                    Timber.d("Launched from notification")
                    launchEvents.add(AttentiveApi.LaunchType.DIRECT_OPEN)
                    // Clear the flag so subsequent resumes from launcher don't trigger another DIRECT_OPEN
                    activity.intent?.removeExtra(LAUNCHED_FROM_NOTIFICATION)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    if (launchEvents.contains(AttentiveApi.LaunchType.DIRECT_OPEN)) {
                        Timber.d("launch even contains DIRECT_OPEN")
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

                        Timber.d("Sending DIRECT_OPEN")
                        AttentiveEventTracker.instance.sendAppLaunchEvent(
                            AttentiveApi.LaunchType.DIRECT_OPEN,
                            dataMap
                        )
                    } else {
                        Timber.d("Sending APP_LAUNCHED")
                        AttentiveEventTracker.instance.sendAppLaunchEvent(AttentiveApi.LaunchType.APP_LAUNCHED)

                    }
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
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
