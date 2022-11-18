package com.attentive.androidsdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AttentiveConfig {
    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;
    private final VisitorService visitorService;
    private UserIdentifiers userIdentifiers;

    public AttentiveConfig(@NonNull String domain, @NonNull Mode mode, @NonNull Context context) {
        ParameterValidation.verifyNotEmpty(domain, "domain");
        ParameterValidation.verifyNotNull(mode, "mode");
        ParameterValidation.verifyNotNull(context, "context");

        this.domain = domain;
        this.mode = mode;
        this.visitorService = new VisitorService(ClassFactory.buildPersistentStorage(context));
    }

    @NonNull
    public Mode getMode() {
        return mode;
    }

    @NonNull
    public String getDomain() {
        return domain;
    }

    @Nullable
    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    @NonNull
    public String getVisitorId() {
        return visitorService.getVisitorId();
    }

    @Deprecated
    public void identify(@NonNull String clientUserId) {
        ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId");

        identify(new UserIdentifiers.Builder().withClientUserId(clientUserId).build());
    }

    public void identify(@NonNull UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        if (this.userIdentifiers == null) {
            this.userIdentifiers = userIdentifiers;
        } else {
            // merge
            this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers);
        }

        sendUserIdentifiersCollectedEvent();
    }

    public void clearUser() {
        this.userIdentifiers = null;
        visitorService.createNewVisitorId();
    }

    private void sendUserIdentifiersCollectedEvent() {
        // TODO
    }
}