package com.attentive.androidsdk;

import java.util.concurrent.Executor;

// TODO name
public class ApiClass {
    private final Executor executor;
    private final AttentiveApiClient attentiveApiClient;

    public ApiClass(Executor executor, AttentiveApiClient attentiveApiClient) {
        this.executor = executor;
        this.attentiveApiClient = attentiveApiClient;
    }

    public void callIdentifyAsync(String domain, UserIdentifiers userIdentifiers) {
        CallIdentify callIdentify = new CallIdentify(domain, userIdentifiers, attentiveApiClient);
        executor.execute(callIdentify);
    }

    private static class CallIdentify implements Runnable {
        private final String domain;
        private final UserIdentifiers userIdentifiers;
        private final AttentiveApiClient attentiveApiClient;

        private CallIdentify(String domain, UserIdentifiers userIdentifiers, AttentiveApiClient attentiveApiClient) {
            this.domain = domain;
            this.userIdentifiers = userIdentifiers;
            this.attentiveApiClient = attentiveApiClient;
        }

        @Override
        public void run() {
            attentiveApiClient.collectUserIdentifiers(domain, userIdentifiers);
        }
    }
}
