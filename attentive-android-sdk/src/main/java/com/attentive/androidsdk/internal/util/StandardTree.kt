package com.attentive.androidsdk.internal.util

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber

class StandardTree : Timber.Tree() {
    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val attentiveMessage = "Attentive: $message"
        if (priority == Log.ERROR) {
            if (t != null) Log.e(tag, attentiveMessage, t) else Log.e(tag, attentiveMessage)
        } else if (priority == Log.WARN) {
            if (t != null) Log.w(tag, attentiveMessage, t) else Log.w(tag, attentiveMessage)
        } else if (priority == Log.INFO) {
            if (t != null) Log.i(tag, attentiveMessage, t) else Log.i(tag, attentiveMessage)
        }
    }
}