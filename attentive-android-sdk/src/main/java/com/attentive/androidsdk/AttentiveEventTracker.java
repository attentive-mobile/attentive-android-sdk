package com.attentive.androidsdk;

import android.util.Log;
import com.attentive.androidsdk.events.Event;


public class AttentiveEventTracker {
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
        Log.i(this.getClass().getName(), "Initializing Attentive SDK with attn domain %s and mode %s");
        ParameterValidation.verifyNotNull(config, "config");

        synchronized (AttentiveEventTracker.class) {
            if (this.config != null) {
                Log.w(this.getClass().getName(), "Attempted to re-initialize AttentiveEventTracker - please initialize once per runtime");
            }

            this.config = config;
        }
    }

    public void recordEvent(Event event) {
        ParameterValidation.verifyNotNull(event, "event");
        verifyInitialized();

        this.config.getAttentiveApi().sendEvent(event, config.getUserIdentifiers(), config.getDomain());
    }

    private void verifyInitialized() {
        synchronized (AttentiveEventTracker.class) {
            if (INSTANCE == null) {
                Log.e(this.getClass().getName(), "AttentiveEventTracker must be initialized before use.");
            }
        }
    }
}
