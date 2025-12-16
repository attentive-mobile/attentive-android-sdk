package com.attentive.androidsdk.internal.util

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber

class VerboseTree : Timber.Tree() {
    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val attentiveMessage = "Attentive: $message"
        if (priority == Log.ERROR) {
            if (t != null) Log.e(tag, attentiveMessage, t) else Log.e(tag, attentiveMessage)
        } else if (priority == Log.WARN) {
            if (t != null) Log.w(tag, attentiveMessage, t) else Log.w(tag, attentiveMessage)
        } else if (priority == Log.INFO) {
            if (t != null) Log.i(tag, attentiveMessage, t) else Log.i(tag, attentiveMessage)
        } else if (priority == Log.DEBUG) {
            if (t != null) Log.d(tag, attentiveMessage, t) else Log.d(tag, attentiveMessage)
        } else if (priority == Log.VERBOSE) {
            if (t != null) Log.v(tag, attentiveMessage, t) else Log.v(tag, attentiveMessage)
        } else if (priority == Log.ASSERT) {
            if (t != null) Log.wtf(tag, attentiveMessage, t) else Log.wtf(tag, attentiveMessage)
        }
    }
}
