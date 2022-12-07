package com.attentive.androidsdk;

import com.attentive.androidsdk.events.Event;

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
