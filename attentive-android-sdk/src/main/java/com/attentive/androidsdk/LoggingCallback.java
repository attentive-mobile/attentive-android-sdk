package com.attentive.androidsdk;

import android.util.Log;

public class LoggingCallback implements AttentiveApiCallback {
    @Override
    public void onFailure(String message) {
        Log.e(this.getClass().getName(), message);
    }

    @Override
    public void onSuccess() {
        Log.i(this.getClass().getName(), "Success");
    }
}
