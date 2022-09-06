package com.attentive.example;

import android.app.Application;
import com.attentive.mobilesdk.AttentiveConfig;

public class ExampleApp extends Application {
    private static final String ATTENTIVE_DOMAIN = "games";
    public AttentiveConfig attentiveConfig;

    @Override
    public void onCreate() {
        attentiveConfig = new AttentiveConfig(
                ATTENTIVE_DOMAIN, AttentiveConfig.Mode.PRODUCTION);
        super.onCreate();
    }
}
