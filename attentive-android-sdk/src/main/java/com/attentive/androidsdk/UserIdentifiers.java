package com.attentive.androidsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserIdentifiers {
    private String clientUserId;
    private String phone;
    private String email;
    private String shopifyId;
    private String klaviyoId;
    private Map<String, String> customIdentifiers;

    private UserIdentifiers() {
    }

    @Nullable
    public String getClientUserId() {
        return clientUserId;
    }

    @Nullable
    public String getPhone() {
        return phone;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getShopifyId() {
        return shopifyId;
    }

    @Nullable
    public String getKlaviyoId() {
        return klaviyoId;
    }

    @NonNull
    public Map<String, String> getCustomIdentifiers() {
        return customIdentifiers;
    }

    public static class Builder {
        private String clientUserId;
        private String phone;
        private String email;
        private String shopifyId;
        private String klaviyoId;
        private Map<String, String> customIdentifiers;

        public Builder withClientUserId(String clientUserId) {
            ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId");
            this.clientUserId = clientUserId;
            return this;
        }

        public Builder withPhone(String phone) {
            ParameterValidation.verifyNotEmpty(phone, "phone");
            this.phone = phone;
            return this;
        }

        public Builder withEmail(String email) {
            ParameterValidation.verifyNotEmpty(email, "email");
            this.email = email;
            return this;
        }

        public Builder withShopifyId(String shopifyId) {
            ParameterValidation.verifyNotEmpty(shopifyId, "shopifyId");
            this.shopifyId = shopifyId;
            return this;
        }

        public Builder withKlaviyoId(String klaviyoId) {
            ParameterValidation.verifyNotEmpty(klaviyoId, "klaviyoId");
            this.klaviyoId = klaviyoId;
            return this;
        }

        public Builder withCustomIdentifiers(Map<String, String> customIdentifiers) {
            ParameterValidation.verifyNotNull(customIdentifiers, "customIdentifiers");
            // Create a new map as a precaution in case other code changes the original map
            this.customIdentifiers = Collections.unmodifiableMap(customIdentifiers);
            return this;
        }

        public UserIdentifiers build() {
            UserIdentifiers userIdentifiers = new UserIdentifiers();
            userIdentifiers.clientUserId = this.clientUserId;
            userIdentifiers.phone = this.phone;
            userIdentifiers.email = this.email;
            userIdentifiers.shopifyId = this.shopifyId;
            userIdentifiers.klaviyoId = this.klaviyoId;
            userIdentifiers.customIdentifiers = this.customIdentifiers == null ? Collections.emptyMap() : this.customIdentifiers;
            return userIdentifiers;
        }
    }

    static UserIdentifiers merge(UserIdentifiers first, UserIdentifiers second) {
        Builder builder = new Builder();

        builder.clientUserId = firstNonNull(second.getClientUserId(), first.getClientUserId());
        builder.phone = firstNonNull(second.getPhone(), first.getPhone());
        builder.email = firstNonNull(second.getEmail(), first.getEmail());
        builder.klaviyoId = firstNonNull(second.getKlaviyoId(), first.getKlaviyoId());
        builder.shopifyId = firstNonNull(second.getShopifyId(), first.getShopifyId());

        Map<String, String> customIdentifiers = new HashMap<>(first.getCustomIdentifiers());
        customIdentifiers.putAll(second.getCustomIdentifiers());
        builder.customIdentifiers = Collections.unmodifiableMap(customIdentifiers);

        return builder.build();
    }

    private static <T> T firstNonNull(T one, T two) {
        if (one != null) {
            return one;
        } else if (two != null) {
            return two;
        } else {
            return null;
        }
    }
}
