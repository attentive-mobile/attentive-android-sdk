package com.attentive.example;

import android.app.Application;
import com.attentive.mobilesdk.AttentiveConfig;

public class ExampleApp extends Application {
    // Change this to your Attentive Domain to test with your Attentive account
    private static final String ATTENTIVE_DOMAIN = "YOUR_ATTENTIVE_DOMAIN";
    // The mode in which to run the Attentive Mobile SDK
    private static final AttentiveConfig.Mode MODE = AttentiveConfig.Mode.PRODUCTION;

    public AttentiveConfig attentiveConfig;

    @Override
    public void onCreate() {
        this.attentiveConfig = new AttentiveConfig(ATTENTIVE_DOMAIN, MODE);
        super.onCreate();
    }
}
