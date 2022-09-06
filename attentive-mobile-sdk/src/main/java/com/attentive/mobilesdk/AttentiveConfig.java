package com.attentive.mobilesdk;

public class AttentiveConfig {

    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;
    private String appUserId;

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

    public String getAppUserId() { return appUserId; }

    public void identify(String appUserId) { this.appUserId = appUserId; }
}