package com.attentive.androidsdk;

public class AttentiveEventTracker {
    private final AttentiveConfig config;

    public AttentiveEventTracker(AttentiveConfig config) {
        ParameterValidation.verifyNotNull(config, "config");

        this.config = config;
    }
}
