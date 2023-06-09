package com.attentive.androidsdk;

import android.util.Log;

import com.attentive.androidsdk.events.Event;

public class AttentiveEventTracker {
    private static final String TAG = "AttentiveEventTracker";
    private static AttentiveEventTracker INSTANCE;

    public static AttentiveEventTracker getInstance() {
        synchronized (AttentiveEventTracker.class) {
            if (INSTANCE == null) {
                INSTANCE = new AttentiveEventTracker();
            }

            return INSTANCE;
        }
    }

    private AttentiveConfig config;

    private AttentiveEventTracker() {
    }

    public void initialize(AttentiveConfig config) {
        synchronized (AttentiveEventTracker.class) {
            if (this.config != null) {
                Log.e(TAG, "AttentiveEventTracker cannot be initialized again");
                return;
            }
            if (config == null) {
                Log.e(TAG, "AttentiveEventTracker must be initialized with non-null config");
                return;
            }

            this.config = config;
        }
    }

    public void recordEvent(Event event) {
        if (event == null) {
            Log.e(TAG, "Will not record null event");
            return;
        }

        if (this.config == null) {
            Log.e(TAG, "AttentiveEventTracker is not initialized. Call initialize() before recording events.");
            return;
        }

        this.config.getAttentiveApi().sendEvent(event, config.getUserIdentifiers(), config.getDomain());
    }
}
