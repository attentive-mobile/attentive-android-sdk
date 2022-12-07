package com.attentive.androidsdk;

public class AttentiveEventTracker {
    private final AttentiveConfig config;

    public AttentiveEventTracker(AttentiveConfig config) {
        ParameterValidation.verifyNotNull(config, "config");

        this.config = config;
    }

    public void recordEvent(Event event) {
        this.config.getAttentiveApi().sendEvent(event, config.getUserIdentifiers(), config.getDomain());
    }
}
