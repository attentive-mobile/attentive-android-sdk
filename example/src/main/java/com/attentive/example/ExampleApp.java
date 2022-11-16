package com.attentive.example;

import android.app.Application;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.UserIdentifiers;

public class ExampleApp extends Application {
    // Change this to your Attentive Domain to test with your Attentive account
    private static final String ATTENTIVE_DOMAIN = "games";
    // The mode in which to run the Attentive Android SDK
    private static final AttentiveConfig.Mode MODE = AttentiveConfig.Mode.PRODUCTION;

    public AttentiveConfig attentiveConfig;

    @Override
    public void onCreate() {
        this.attentiveConfig = new AttentiveConfig(ATTENTIVE_DOMAIN, MODE, this);

        // Register the current user with the Attentive SDK. This should be done as early as possible.
        // Replace "APP_USER_ID" with the current user's ID.
        UserIdentifiers userIdentifiers =
            new UserIdentifiers.Builder()
                .withClientUserId("222")
                .withEmail("wyatt222@gmail.com")
                .withPhone("+15415132222")
                .build();
        attentiveConfig.identify(userIdentifiers);

        super.onCreate();
    }
}
