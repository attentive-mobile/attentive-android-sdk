package com.attentive.androidsdk.creatives

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.annotation.RestrictTo
import timber.log.Timber

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class CreativeActivityCallbacks(creative: Creative) : ActivityLifecycleCallbacks {
    private var creative: Creative?

    init {
        this.creative = creative
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        // No-op
    }

    override fun onActivityStarted(activity: Activity) {
        // No-op
    }

    override fun onActivityResumed(activity: Activity) {
        // No-op
    }

    override fun onActivityPaused(activity: Activity) {
        // No-op
    }

    override fun onActivityStopped(activity: Activity) {
        // No-op
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {
        // No-op
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (creative != null) {
            Timber.d("onActivityDestroyed called")
            creative!!.destroy()
        }
        creative = null
    }
}
