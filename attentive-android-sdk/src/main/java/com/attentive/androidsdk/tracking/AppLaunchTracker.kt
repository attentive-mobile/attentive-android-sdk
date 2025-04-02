package com.attentive.androidsdk.tracking

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.attentive.androidsdk.AttentiveEventTracker
import timber.log.Timber

class AppLaunchTracker() : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("App moved to foreground")
    }

    fun registerAppLaunchTracker() {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(this)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Timber.d("App was already in foreground at registration")
            onStart(ProcessLifecycleOwner.get())
        }
    }
}