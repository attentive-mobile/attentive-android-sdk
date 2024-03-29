package com.attentive.androidsdk;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.attentive.androidsdk.internal.events.InfoEvent;
import okhttp3.OkHttpClient;

public class AttentiveConfig {
    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;
    private final VisitorService visitorService;
    private UserIdentifiers userIdentifiers;
    private AttentiveApi attentiveApi;

    public AttentiveConfig(@NonNull String domain, @NonNull Mode mode, @NonNull Context context) {
        this(domain, mode, context, ClassFactory.buildOkHttpClient(ClassFactory.buildUserAgentInterceptor(context)));
    }

    public AttentiveConfig(@NonNull String domain, @NonNull Mode mode, @NonNull Context context, @NonNull OkHttpClient okHttpClient) {
        ParameterValidation.verifyNotEmpty(domain, "domain");
        ParameterValidation.verifyNotNull(mode, "mode");
        ParameterValidation.verifyNotNull(context, "context");
        ParameterValidation.verifyNotNull(okHttpClient, "okHttpClient");

        this.domain = domain;
        this.mode = mode;
        this.visitorService = ClassFactory.buildVisitorService(ClassFactory.buildPersistentStorage(context));
        this.attentiveApi = ClassFactory.buildAttentiveApi(okHttpClient, ClassFactory.buildObjectMapper());

        this.userIdentifiers = new UserIdentifiers.Builder().withVisitorId(visitorService.getVisitorId()).build();

        sendInfoEvent();
    }

    @NonNull
    public Mode getMode() {
        return mode;
    }

    @NonNull
    public String getDomain() {
        return domain;
    }

    @NonNull
    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    @NonNull
    AttentiveApi getAttentiveApi() {
        return this.attentiveApi;
    }

    @Deprecated
    public void identify(@NonNull String clientUserId) {
        ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId");

        identify(new UserIdentifiers.Builder().withClientUserId(clientUserId).build());
    }

    public void identify(@NonNull UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers);

        sendUserIdentifiersCollectedEvent();
    }

    public void clearUser() {
        String newVisitorId = visitorService.createNewVisitorId();
        this.userIdentifiers = new UserIdentifiers.Builder().withVisitorId(newVisitorId).build();
    }

    private void sendUserIdentifiersCollectedEvent() {
        attentiveApi.sendUserIdentifiersCollectedEvent(getDomain(), getUserIdentifiers(), new AttentiveApiCallback() {
            private static final String tag = "AttentiveConfig";

            @Override
            public void onFailure(String message) {
                Log.e(tag, "Could not send the user identifiers. Error: " + message);
            }

            @Override
            public void onSuccess() {
                Log.i(tag, "Successfully sent the user identifiers");
            }
        });
    }

    // Send an Info event to collect some telemetry/analytics
    private void sendInfoEvent() {
        attentiveApi.sendEvent(new InfoEvent(), getUserIdentifiers(), getDomain());
    }
}