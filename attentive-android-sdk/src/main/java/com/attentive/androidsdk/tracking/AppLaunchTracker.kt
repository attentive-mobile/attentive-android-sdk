package com.attentive.androidsdk.tracking

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import timber.log.Timber

class AppLaunchTracker(internal val lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("App moved to foreground")
        AttentiveEventTracker.instance.config?.context?.let {
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
}