package com.attentive.androidsdk.internal.util

import android.annotation.SuppressLint
import android.util.Log
import timber.log.Timber

class VerboseTree : Timber.Tree() {
    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR) {
            Log.e(tag, message, t)
        } else if (priority == Log.WARN) {
            Log.w(tag, message, t)
        } else if (priority == Log.INFO) {
            Log.i(tag, message, t)
        } else if (priority == Log.DEBUG) {
            Log.d(tag, message, t)
        } else if (priority == Log.VERBOSE) {
            Log.v(tag, message, t)
        } else if (priority == Log.ASSERT) {
            Log.wtf(tag, message, t)
        }
    }
}
