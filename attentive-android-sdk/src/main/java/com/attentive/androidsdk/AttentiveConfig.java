package com.attentive.androidsdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;

public class AttentiveConfig {

    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;
    private final AttentiveApi attentiveApi;
    private String appUserId;
    private UserIdentifiers userIdentifiers;

    public AttentiveConfig(String domain, Mode mode) {
        this.domain = domain;
        this.mode = mode;

        // TODO make this injectable
        ObjectMapper objectMapper = new ObjectMapper();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        OkHttpClient okHttpClient = new OkHttpClient();
        this.attentiveApi = new AttentiveApi(executorService, okHttpClient, objectMapper);
    }

    public Mode getMode() {
        return mode;
    }

    public String getDomain() {
        return domain;
    }

    public String getAppUserId() { return appUserId; }

    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    public void identify(String appUserId) {
        identify(appUserId, null);
    }

    public void identify(String appUserId, UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotEmpty(appUserId, "appUserId");

        this.appUserId = appUserId;

        if (userIdentifiers == null) {
            this.userIdentifiers = new UserIdentifiers.Builder(appUserId).build();
        } else {
            this.userIdentifiers = userIdentifiers;
        }

        attentiveApi.callIdentifyAsync(domain, this.userIdentifiers);
    }
}