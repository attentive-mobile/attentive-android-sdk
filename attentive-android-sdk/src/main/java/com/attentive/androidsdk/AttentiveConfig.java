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
    private UserIdentifiers userIdentifiers;

    public AttentiveConfig(@NonNull String domain, @NonNull Mode mode, @NonNull Context context) {
        ParameterValidation.verifyNotEmpty(domain, "domain");
        ParameterValidation.verifyNotNull(mode, "mode");
        ParameterValidation.verifyNotNull(context, "context");

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
    public String getClientUserId() {
        return userIdentifiers == null ? null : userIdentifiers.getClientUserId();
    }

    @Nullable
    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    @Deprecated
    public void identify(@NonNull String appUserId) {
        ParameterValidation.verifyNotEmpty(appUserId, "appUserId");

        identify(new UserIdentifiers.Builder().withClientUserId(appUserId).build());
    }

    public void identify(@NonNull UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        if (this.userIdentifiers == null) {
            this.userIdentifiers = userIdentifiers;
        } else {
            // merge
            this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers);
        }
    }

    public void clearUser() {
        this.userIdentifiers = null;
    }
}