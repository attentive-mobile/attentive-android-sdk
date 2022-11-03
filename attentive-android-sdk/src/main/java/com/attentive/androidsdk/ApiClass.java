package com.attentive.androidsdk;

public class ApiClass {
    public void callIdentifyAsync(String domain, UserIdentifiers userIdentifiers) {
        CallIdentify callIdentify = new CallIdentify(domain, userIdentifiers);
    }

    private static class CallIdentify implements Runnable {
        private final String domain;
        private final UserIdentifiers userIdentifiers;

        private CallIdentify(String domain, UserIdentifiers userIdentifiers) {
            this.domain = domain;
            this.userIdentifiers = userIdentifiers;
        }

        @Override
        public void run() {
            // eventsApi.setIdentifiers(userIdentifiers);
            // eventsApi.identify(domain, userIdentifiers);
            // eventsApi.addToCart(domain, userIdentifiers, doIt!
            // client.identify(domain, userIdentifiers);
        }
    }
}
