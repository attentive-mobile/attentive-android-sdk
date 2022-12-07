package com.attentive.androidsdk;

public class AttentiveEventTracker {
    private static AttentiveEventTracker INSTANCE;

    public static void initialize(AttentiveConfig config) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Initialize already called");
        }

        INSTANCE = new AttentiveEventTracker(config);
    }

    public static AttentiveEventTracker getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Initialize must be called");
        }

        return INSTANCE;
    }

    private final AttentiveConfig config;

    private AttentiveEventTracker(AttentiveConfig config) {
        this.config = config;
    }

    public void recordEvent(Event event) {
        this.config.getAttentiveApi().sendEvent(event, config.getUserIdentifiers(), config.getDomain());
    }
}
