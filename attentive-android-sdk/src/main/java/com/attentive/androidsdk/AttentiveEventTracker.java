package com.attentive.androidsdk;

import com.attentive.androidsdk.events.Event;
import timber.log.Timber;

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
        ParameterValidation.verifyNotNull(config, "config");
        Timber.i("Initializing Attentive SDK with attn domain %s and mode %s", config.getDomain(), config.getMode());

        synchronized (AttentiveEventTracker.class) {
            if (this.config != null) {
                Timber.w("Attempted to re-initialize AttentiveEventTracker - please initialize once per runtime");
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
                Timber.e("AttentiveEventTracker must be initialized before use.");
            }
        }
    }
}
