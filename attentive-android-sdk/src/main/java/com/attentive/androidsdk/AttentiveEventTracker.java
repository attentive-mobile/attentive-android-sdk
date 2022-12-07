package com.attentive.androidsdk;

import com.attentive.androidsdk.events.Event;

public class AttentiveEventTracker {
    private static AttentiveEventTracker INSTANCE;

    public static AttentiveEventTracker getInstance() {
        synchronized (AttentiveEventTracker.class) {
            if (INSTANCE == null) {
                throw new IllegalStateException("AttentiveEventTrack is not initialized");
            }

            return INSTANCE;
        }
    }

    public static void initialize(AttentiveConfig config) {
        synchronized (AttentiveEventTracker.class) {
            if (INSTANCE != null) {
                throw new IllegalStateException("AttentiveEventTracker is not initialized");
            }

            INSTANCE = new AttentiveEventTracker(config);
        }
    }

    private final AttentiveConfig config;

    private AttentiveEventTracker(AttentiveConfig config) {
        ParameterValidation.verifyNotNull(config, "config");

        this.config = config;
    }

    public void recordEvent(Event event) {
        this.config.getAttentiveApi().sendEvent(event, config.getUserIdentifiers(), config.getDomain());
    }
}
