package com.attentive.androidsdk;

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
        ParameterValidation.verifyNotNull(config, "config");

        synchronized (AttentiveEventTracker.class) {
            if (this.config != null) {
                throw new IllegalStateException("AttentiveEventTracker cannot be initialized again");
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
                throw new IllegalStateException("AttentiveEventTracker must be initialized before use.");
            }
        }
    }
}
