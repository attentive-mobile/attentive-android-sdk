package com.attentive.androidsdk;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.attentive.androidsdk.internal.events.InfoEvent;
import java.util.Objects;
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
    private final AttentiveApi attentiveApi;
    private boolean skipFatigueOnCreatives;
    private final SettingsService settingsService;

    private AttentiveConfig(@NonNull Builder builder) {
        Context context = builder.context;
        this.mode = builder.mode;
        this.domain = builder.domain;
        this.visitorService = ClassFactory.buildVisitorService(ClassFactory.buildPersistentStorage(context));
        this.settingsService = ClassFactory.buildSettingsService(ClassFactory.buildPersistentStorage(context));
        this.userIdentifiers = new UserIdentifiers.Builder().withVisitorId(visitorService.getVisitorId()).build();
        OkHttpClient okHttpClient = builder.okHttpClient;
        if (okHttpClient == null) {
            okHttpClient = ClassFactory.buildOkHttpClient(ClassFactory.buildUserAgentInterceptor(context));
        }
        this.attentiveApi = ClassFactory.buildAttentiveApi(okHttpClient, ClassFactory.buildObjectMapper());
        this.skipFatigueOnCreatives = builder.skipFatigueOnCreatives;

        sendInfoEvent();
    }

    /**
     * @deprecated  As of release 0.4.6, replaced by {@link AttentiveConfig.Builder}
     */
    @Deprecated(since = "0.4.6", forRemoval = true)
    public AttentiveConfig(@NonNull String domain, @NonNull Mode mode, @NonNull Context context) {
        this(domain, mode, context, ClassFactory.buildOkHttpClient(ClassFactory.buildUserAgentInterceptor(context)));
    }

    /**
     * @deprecated  As of release 0.4.6, replaced by {@link AttentiveConfig.Builder}
     */
    @Deprecated(since = "0.4.6", forRemoval = true)
    public AttentiveConfig(@NonNull String domain, @NonNull Mode mode, @NonNull Context context, @NonNull OkHttpClient okHttpClient) {
        ParameterValidation.verifyNotEmpty(domain, "domain");
        ParameterValidation.verifyNotNull(mode, "mode");
        ParameterValidation.verifyNotNull(context, "context");
        ParameterValidation.verifyNotNull(okHttpClient, "okHttpClient");

        this.domain = domain;
        this.mode = mode;
        this.visitorService = ClassFactory.buildVisitorService(ClassFactory.buildPersistentStorage(context));
        this.settingsService = ClassFactory.buildSettingsService(ClassFactory.buildPersistentStorage(context));
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

    public boolean skipFatigueOnCreatives() {
        if (this.settingsService != null) {
            Boolean skipFatigueOnCreatives = this.settingsService.isSkipFatigueEnabled();
            return Objects.requireNonNullElseGet(skipFatigueOnCreatives, () -> this.skipFatigueOnCreatives);
        }
        return this.skipFatigueOnCreatives;
    }

    /**
     * @deprecated  As of release 0.2.0, replaced by {@link #identify(UserIdentifiers)}
     * @param clientUserId The client user id.
     */
    @Deprecated
    public void identify(@NonNull String clientUserId) {
        ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId");

        identify(new UserIdentifiers.Builder().withClientUserId(clientUserId).build());
    }

    /**
     * Method that sets the identifiers from the user to be used on the next events/actions.
     * @param userIdentifiers {@link UserIdentifiers} that have the user information to be used.
     */
    public void identify(@NonNull UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers);

        sendUserIdentifiersCollectedEvent();
    }

    /**
     * Method that clears all of the old use information and creates a fresh new one to track
     * following events/actions.
     */
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

    public static class Builder {
        private Context context;
        private Mode mode;
        private String domain;
        private OkHttpClient okHttpClient;
        private boolean skipFatigueOnCreatives;

        public Builder context(@NonNull Context context) {
            ParameterValidation.verifyNotNull(context, "context");
            this.context = context;
            return this;
        }

        public Builder mode(@NonNull Mode mode) {
            ParameterValidation.verifyNotNull(mode, "mode");
            this.mode = mode;
            return this;
        }

        public Builder domain(@NonNull String domain) {
            ParameterValidation.verifyNotEmpty(domain, "domain");
            this.domain = domain;
            return this;
        }

        public Builder okHttpClient(@NonNull OkHttpClient okHttpClient) {
            ParameterValidation.verifyNotNull(okHttpClient, "okHttpClient");
            this.okHttpClient = okHttpClient;
            return this;
        }

        public Builder skipFatigueOnCreatives(boolean skipFatigueOnCreatives) {
            this.skipFatigueOnCreatives = skipFatigueOnCreatives;
            return this;
        }

        public AttentiveConfig build() {
            if (this.mode == null) {
                throw new IllegalStateException("A valid mode must be provided.");
            }
            if (this.domain == null) {
                throw new IllegalStateException("A valid domain must be provided.");
            }
            if (this.context == null) {
                throw new IllegalStateException("A valid context must be provided.");
            }
            return new AttentiveConfig(this);
        }
    }
}
