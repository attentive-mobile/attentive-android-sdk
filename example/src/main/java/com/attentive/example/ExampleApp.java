package com.attentive.example;

import android.app.Application;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.AttentiveEventTracker;
import com.attentive.androidsdk.AttentiveLogLevel;
import com.attentive.androidsdk.BuildConfig;
import com.attentive.androidsdk.UserIdentifiers;
import java.util.Map;

public class ExampleApp extends Application {
    // The mode in which to run the Attentive Android SDK
    private static final AttentiveConfig.Mode MODE = AttentiveConfig.Mode.PRODUCTION;

    private AttentiveConfig attentiveConfig;

    @Override
    public void onCreate() {
        super.onCreate();

        // Change this to your Attentive Domain to test with your Attentive account
        final String attentiveDomain = getString(R.string.default_domain);

        // Initialize the Attentive SDK. This only has to be done once per application lifecycle.
        this.attentiveConfig = new AttentiveConfig.Builder()
                .context(getApplicationContext())
                .domain(attentiveDomain)
                .mode(MODE)
                .logLevel(AttentiveLogLevel.LIGHT)
                .build();

        // AttentiveEventTracker's "initialize" must be called before the AttentiveEventTracker can be used to send
        // events. The method "initialize" only needs to be called once.
        AttentiveEventTracker.getInstance().initialize(attentiveConfig);

        // Register the current user with the Attentive SDK. This should be done as early as possible.
        attentiveConfig.identify(buildUserIdentifiers());
    }

    public AttentiveConfig getAttentiveConfig() {
        return attentiveConfig;
    }

    public void setAttentiveConfig(AttentiveConfig newConfig) {
        // In a production setting, the AttentiveConfig should only be created once per application
        // lifecycle. This method provides functionality for recreating the AttentiveConfig for easy
        // debugging & testing purposes.
        attentiveConfig = newConfig;
    }

    public static UserIdentifiers buildUserIdentifiers() {
        // Add all the identifiers that you have for the current user. All identifiers are
        // optional, but the more you add the better the Attentive SDK will function.
        return new UserIdentifiers.Builder()
            .withPhone("+15556667777")
            .withEmail("some_email@gmailfake.com")
            .withKlaviyoId("userKlaviyoId")
            .withShopifyId("userShopifyId")
            .withClientUserId("userClientUserId")
            .withCustomIdentifiers(Map.of("customIdentifierKey", "customIdentifierValue"))
            .build();
    }
}
