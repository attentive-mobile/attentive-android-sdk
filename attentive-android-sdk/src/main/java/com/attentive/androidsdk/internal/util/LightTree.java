package com.attentive.androidsdk.internal.util;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class LightTree extends Timber.Tree {
    @SuppressLint("LogNotTimber")
    @Override
    protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
        if (priority == Log.ERROR) {
            Log.e(tag, message, t);
        } else if (priority == Log.WARN) {
            Log.w(tag, message, t);
        }
    }
}
