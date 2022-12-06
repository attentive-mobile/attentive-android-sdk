package com.attentive.androidsdk;

public class AttentiveAnalytics {
    private static AttentiveAnalytics INSTANCE;

    private AttentiveConfig config;

    private AttentiveAnalytics(AttentiveConfig config) {
        this.config = config;
    }

    public static AttentiveAnalytics getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Not initialized");
        }

        return INSTANCE;
    }

    public static void initialize(AttentiveConfig config) {
        synchronized (AttentiveAnalytics.class) {
            if (INSTANCE != null) {
                throw new IllegalStateException("Already initialized");
            }

            INSTANCE = new AttentiveAnalytics(config);
        }
    }

    public void recordPurchase(Purchase purchase) {
        // TODO
    }
}
