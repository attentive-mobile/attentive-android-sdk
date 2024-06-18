package com.attentive.androidsdk;

import static com.attentive.androidsdk.internal.util.AppInfo.isDebuggable;

import android.content.Context;
import com.attentive.androidsdk.internal.events.InfoEvent;
import com.attentive.androidsdk.internal.util.LightTree;
import com.attentive.androidsdk.internal.util.StandardTree;
import com.attentive.androidsdk.internal.util.VerboseTree;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import timber.log.Timber;

public class AttentiveConfig {
    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private String domain;
    private final VisitorService visitorService;
    private UserIdentifiers userIdentifiers;
    private final AttentiveApi attentiveApi;
    private boolean skipFatigueOnCreatives;
    private final SettingsService settingsService;

    private AttentiveConfig(@NotNull Builder builder) {
        Context context = builder.context;
        this.settingsService = ClassFactory.buildSettingsService(ClassFactory.buildPersistentStorage(context));
        configureLogging(builder.logLevel, settingsService, context);

        Timber.d("Initializing AttentiveConfig with configuration: %s", builder);
        this.mode = builder.mode;
        this.domain = builder.domain;
        this.visitorService = ClassFactory.buildVisitorService(ClassFactory.buildPersistentStorage(context));
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
    public AttentiveConfig(@NotNull String domain, @NotNull Mode mode, @NotNull Context context) {
        this(domain, mode, context, ClassFactory.buildOkHttpClient(ClassFactory.buildUserAgentInterceptor(context)));
    }

    /**
     * @deprecated  As of release 0.4.6, replaced by {@link AttentiveConfig.Builder}
     */
    @Deprecated(since = "0.4.6", forRemoval = true)
    public AttentiveConfig(@NotNull String domain, @NotNull Mode mode, @NotNull Context context, @NotNull OkHttpClient okHttpClient) {
        this.settingsService = ClassFactory.buildSettingsService(ClassFactory.buildPersistentStorage(context));
        configureLogging(AttentiveLogLevel.VERBOSE, settingsService, context);
        Timber.d("Initializing AttentiveConfig with the following configuration: domain=%s, mode=%s, okHttpClient=%s", domain, mode, okHttpClient);
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

    @NotNull
    public Mode getMode() {
        return mode;
    }

    @NotNull
    public String getDomain() {
        return domain;
    }

    @NotNull
    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    @NotNull
    AttentiveApi getAttentiveApi() {
        return this.attentiveApi;
    }

    public boolean skipFatigueOnCreatives() {
        Timber.d("skipFatigueOnCreatives called");
        if (this.settingsService != null) {
            Boolean skipFatigueOnCreatives = this.settingsService.isSkipFatigueEnabled();
            return skipFatigueOnCreatives == null ? this.skipFatigueOnCreatives : skipFatigueOnCreatives;
        }
        return this.skipFatigueOnCreatives;
    }

    /**
     * @deprecated  As of release 0.2.0, replaced by {@link #identify(UserIdentifiers)}
     * @param clientUserId The client user id.
     */
    @Deprecated
    public void identify(@NotNull String clientUserId) {
        Timber.d("identify called with clientUserId: %s", clientUserId);
        ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId");

        identify(new UserIdentifiers.Builder().withClientUserId(clientUserId).build());
    }

    /**
     * Method that sets the identifiers from the user to be used on the next events/actions.
     * @param userIdentifiers {@link UserIdentifiers} that have the user information to be used.
     */
    public void identify(@NotNull UserIdentifiers userIdentifiers) {
        Timber.d("identify called with userIdentifiers: %s", userIdentifiers);
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers);

        sendUserIdentifiersCollectedEvent();
    }

    /**
     * Method that clears all of the old use information and creates a fresh new one to track
     * following events/actions.
     */
    public void clearUser() {
        Timber.d("clearUser called");
        String newVisitorId = visitorService.createNewVisitorId();
        this.userIdentifiers = new UserIdentifiers.Builder().withVisitorId(newVisitorId).build();
    }
    /**
     * Method that changes the domain of the Attentive instance. It will change the domain for
     * future use within the Attentive SDK.
     * @param domain The new domain. Should not be the same domain that it's set to nor an empty
     *               string
     */
    public void changeDomain(@NotNull String domain) {
        if (domain.isEmpty()) {
            return;
        }
        if (domain.equals(this.domain)) {
            return;
        }

        this.domain = domain;
        sendInfoEvent();
    }

    @NotNull
    @Override
    public String toString() {
        return "AttentiveConfig{" +
                "mode=" + mode +
                ", domain='" + domain + '\'' +
                ", visitorService=" + visitorService +
                ", userIdentifiers=" + userIdentifiers +
                ", attentiveApi=" + attentiveApi +
                ", skipFatigueOnCreatives=" + skipFatigueOnCreatives +
                ", settingsService=" + settingsService +
                '}';
    }

    private void sendUserIdentifiersCollectedEvent() {
        attentiveApi.sendUserIdentifiersCollectedEvent(getDomain(), getUserIdentifiers(), new AttentiveApiCallback() {

            @Override
            public void onFailure(String message) {
                Timber.e("Could not send the user identifiers. Error: %s", message);
            }

            @Override
            public void onSuccess() {
                Timber.i("Successfully sent the user identifiers");
            }
        });
    }

    // Send an Info event to collect some telemetry/analytics
    private void sendInfoEvent() {
        attentiveApi.sendEvent(new InfoEvent(), getUserIdentifiers(), getDomain());
    }

    private static void configureLogging(@Nullable AttentiveLogLevel logLevel, @NotNull SettingsService settingsService, @NotNull Context context) {
        if (!isDebuggable(context)) {
            Timber.plant(new LightTree());
            return;
        }
        AttentiveLogLevel settingsLogLevel = settingsService.getLogLevel();
        if (settingsLogLevel != null) {
            setLogLevel(settingsLogLevel);
            return;
        }
        if (logLevel != null) {
            setLogLevel(logLevel);
            return;
        }
        if (isDebuggable(context)) {
            setLogLevel(AttentiveLogLevel.VERBOSE);
        }
    }

    private static void setLogLevel(@NotNull AttentiveLogLevel logLevel) {
        if (logLevel == AttentiveLogLevel.VERBOSE) {
            Timber.plant(new VerboseTree());
        } else if (logLevel == AttentiveLogLevel.STANDARD) {
            Timber.plant(new StandardTree());
        } else if (logLevel == AttentiveLogLevel.LIGHT) {
            Timber.plant(new LightTree());
        }
    }

    public static class Builder {
        private Context context;
        private Mode mode;
        private String domain;
        private OkHttpClient okHttpClient;
        private boolean skipFatigueOnCreatives;
        private AttentiveLogLevel logLevel;

        public Builder context(@NotNull Context context) {
            ParameterValidation.verifyNotNull(context, "context");
            this.context = context;
            return this;
        }

        public Builder mode(@NotNull Mode mode) {
            ParameterValidation.verifyNotNull(mode, "mode");
            this.mode = mode;
            return this;
        }

        public Builder domain(@NotNull String domain) {
            ParameterValidation.verifyNotEmpty(domain, "domain");
            this.domain = domain;
            return this;
        }

        public Builder okHttpClient(@NotNull OkHttpClient okHttpClient) {
            ParameterValidation.verifyNotNull(okHttpClient, "okHttpClient");
            this.okHttpClient = okHttpClient;
            return this;
        }

        public Builder skipFatigueOnCreatives(boolean skipFatigueOnCreatives) {
            this.skipFatigueOnCreatives = skipFatigueOnCreatives;
            return this;
        }

        public Builder logLevel(AttentiveLogLevel logLevel) {
            this.logLevel = logLevel;
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

        @Override
        @NotNull
        public String toString() {
            return "Builder{" +
                    "context=" + context +
                    ", mode=" + mode +
                    ", domain='" + domain + '\'' +
                    ", okHttpClient=" + okHttpClient +
                    ", skipFatigueOnCreatives=" + skipFatigueOnCreatives +
                    ", logLevel=" + logLevel +
                    '}';
        }
    }
}
