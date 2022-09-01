package com.attentive.mobilesdk;

public class AttentiveConfig {

    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;

    public AttentiveConfig(String domain, Mode mode) {
        this.domain = domain;
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public String getDomain() {
        return domain;
    }
}