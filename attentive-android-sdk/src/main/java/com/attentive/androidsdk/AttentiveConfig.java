package com.attentive.androidsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AttentiveConfig {

    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;
    private UserIdentifiers userIdentifiers;

    public AttentiveConfig(@NonNull String domain, @NonNull Mode mode) {
        ParameterValidation.verifyNotEmpty(domain, "domain");
        ParameterValidation.verifyNotNull(mode, "mode");

        this.domain = domain;
        this.mode = mode;
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
    public String getAppUserId() {
        return userIdentifiers == null ? null : userIdentifiers.getAppUserId();
    }

    @Nullable
    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    @Deprecated
    public void identify(@NonNull String appUserId) {
        ParameterValidation.verifyNotEmpty(appUserId, "appUserId");

        identify(new UserIdentifiers.Builder(appUserId).build());
    }

    public void identify(@NonNull UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        if (this.userIdentifiers == null || !this.userIdentifiers.getAppUserId().equals(userIdentifiers.getAppUserId())) {
            this.userIdentifiers = userIdentifiers;
        } else {
            // merge
            this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers);
        }

        sendUserIdentifiersCollectedEvent();
    }

    public void clearUser() {
        this.userIdentifiers = null;
    }

    private void sendUserIdentifiersCollectedEvent() {
        // TODO
    }
}