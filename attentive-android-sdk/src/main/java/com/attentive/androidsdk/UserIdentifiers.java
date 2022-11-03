package com.attentive.androidsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class UserIdentifiers {
    private final String appUserId;
    private String phone;
    private String email;
    private String shopifyId;
    private String klaviyoId;
    private List<CustomIdentifier> customIdentifiers;

    private UserIdentifiers(@NonNull String appUserId) {
        ParameterValidation.verifyNotEmpty(appUserId, "appUserId");

        this.appUserId = appUserId;
    }

    @NonNull
    public String getAppUserId() {
        return appUserId;
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

    @Nullable
    public List<CustomIdentifier> getCustomIdentifiers() {
        return customIdentifiers;
    }

    public static class CustomIdentifier {
        private final String name;
        private final String value;

        public CustomIdentifier(String name, String value) {
            ParameterValidation.verifyNotEmpty(name, "name");
            ParameterValidation.verifyNotEmpty(value, "value");

            this.name = name;
            this.value = value;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @NonNull
        public String getValue() {
            return value;
        }
    }

    public static class Builder {
        private final String appUserId;
        private String phone;
        private String email;
        private String shopifyId;
        private String klaviyoId;
        private List<CustomIdentifier> customIdentifiers;

        public Builder(String appUserId) {
            ParameterValidation.verifyNotEmpty(appUserId, "appUserId");

            this.appUserId = appUserId;
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

        public Builder withCustomIdentifiers(List<CustomIdentifier> customIdentifiers) {
            ParameterValidation.verifyNotNull(customIdentifiers, "customIdentifiers");
            // Create a new List as a precaution in case the original list changes
            this.customIdentifiers = new ArrayList<>(customIdentifiers);
            return this;

        }

        public UserIdentifiers build() {
            UserIdentifiers userIdentifiers = new UserIdentifiers(appUserId);
            userIdentifiers.phone = this.phone;
            userIdentifiers.email = this.email;
            userIdentifiers.shopifyId = this.shopifyId;
            userIdentifiers.klaviyoId = this.klaviyoId;
            userIdentifiers.customIdentifiers = this.customIdentifiers;
            return userIdentifiers;
        }
    }
}
