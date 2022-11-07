package com.attentive.androidsdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AttentiveConfig {

    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;
    private UserIdentifiers userIdentifiers;
    private final ApiClass apiClass;

    public AttentiveConfig(String domain, Mode mode) {
        this.domain = domain;
        this.mode = mode;

        // TODO make this injectable
        LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, blockingQueue);
        HttpClient httpClient = new HttpClient();
        AttentiveApiClient attentiveApiClient = new AttentiveApiClient(httpClient, new ObjectMapper());
        this.apiClass = new ApiClass(threadPoolExecutor, attentiveApiClient);
    }

    public Mode getMode() {
        return mode;
    }

    public String getDomain() {
        return domain;
    }

    public String getAppUserId() { return userIdentifiers.getAppUserId(); }

    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    public void identify(String appUserId) {
        ParameterValidation.verifyNotEmpty(appUserId, "appUserId");

        identify(new UserIdentifiers.Builder(appUserId).build());
    }

    public void identify(UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        this.userIdentifiers = userIdentifiers;
        callIdentifyApi();
    }

    private void callIdentifyApi() {
        apiClass.callIdentifyAsync(domain, userIdentifiers);
    }
}