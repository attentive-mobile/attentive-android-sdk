package com.attentive.androidsdk.internal.util

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber

class LightTree : Timber.Tree() {
    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val attentiveMessage = "Attentive: $message"
        if (priority == Log.ERROR) {
            Log.e(tag, attentiveMessage, t)
        } else if (priority == Log.WARN) {
            Log.w(tag, attentiveMessage, t)
        }
    }
}
