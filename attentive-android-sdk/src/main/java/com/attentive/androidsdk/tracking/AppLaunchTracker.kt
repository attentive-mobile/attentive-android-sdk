package com.attentive.androidsdk.tracking

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AppLaunchTracker(internal val lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("App moved to foreground")

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

            AttentiveEventTracker.instance.config?.context?.let {
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

    companion object {
        var wasLaunchedFromNotification = false
    }
}