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
import timber.log.Timber

internal class AppLaunchTracker(
    internal val application: Application,
    internal val lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle,
) : DefaultLifecycleObserver {
    init {
        registerAppLaunchTracker()
        registerActivityCallback()
    }

    val launchEvents = mutableListOf<AttentiveApi.LaunchType>()
    var dataMap = mutableMapOf<String, String>()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        AttentiveEventTracker.instance.config?.applicationContext?.let {
            CoroutineScope(Dispatchers.IO).launch {
                AttentiveEventTracker.instance.registerPushToken(it)
            }
        }
    }

    fun registerAppLaunchTracker() {
        lifecycle.addObserver(this)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            onStart(ProcessLifecycleOwner.get())
        }
    }

    private fun registerActivityCallback() {
        application.registerActivityLifecycleCallbacks(
            object :
                Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {
                    var wasLaunchedFromNotification =
                        activity.intent?.extras?.run {
                            getBoolean(LAUNCHED_FROM_NOTIFICATION, false)
                        } == true

                    if (wasLaunchedFromNotification) {
                        Timber.i("Launched from notification")
                        launchEvents.add(AttentiveApi.LaunchType.DIRECT_OPEN)
                    } else {
                        Timber.i("Launched from launcher")
                    }
                }

                override fun onActivityStarted(activity: Activity) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (launchEvents.contains(AttentiveApi.LaunchType.DIRECT_OPEN)) {
                            activity.intent.extras.run {
                                if (this != null) {
                                    for (key in keySet()) {
                                        // All the metadata for the notification is packaged in the launch intent
                                        // The attentive backend needs these to be sent back to it when a notification is tapped
                                        // The LAUNCHED_FROM_NOTIFICATION flag is a flag used for internal use only and shouldn't be sent to the backend
                                        if (key != LAUNCHED_FROM_NOTIFICATION) {
                                            dataMap[key] = getString(key).toString()
                                        }
                                    }
                                }
                            }

                            jsonEncodeTitleAndBody()

                            AttentiveEventTracker.instance.sendAppLaunchEvent(
                                AttentiveApi.LaunchType.DIRECT_OPEN,
                                dataMap,
                            )
                        } else {
                            AttentiveEventTracker.instance.sendAppLaunchEvent(AttentiveApi.LaunchType.APP_LAUNCHED)
                        }
                    }
                }

                override fun onActivityStopped(activity: Activity) {
                    launchEvents.clear()
                }

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) {}

                override fun onActivityDestroyed(activity: Activity) {}
            },
        )
    }

    fun jsonEncodeTitleAndBody() {
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
