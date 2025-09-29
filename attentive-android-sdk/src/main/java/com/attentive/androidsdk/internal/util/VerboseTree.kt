package com.attentive.androidsdk.internal.util

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber

class VerboseTree : Timber.Tree() {
    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val attentiveMessage = "Attentive: $message"
        if (priority == Log.ERROR) {
            Log.e(tag, attentiveMessage, t)
        } else if (priority == Log.WARN) {
            Log.w(tag, attentiveMessage, t)
        } else if (priority == Log.INFO) {
            Log.i(tag, attentiveMessage, t)
        } else if (priority == Log.DEBUG) {
            Log.d(tag, attentiveMessage, t)
        } else if (priority == Log.VERBOSE) {
            Log.v(tag, attentiveMessage, t)
        } else if (priority == Log.ASSERT) {
            Log.wtf(tag, attentiveMessage, t)
        }
    }
}
