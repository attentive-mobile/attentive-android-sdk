package com.attentive.androidsdk.internal.util;

import android.net.Uri;
import android.util.Log;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.UserIdentifiers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CreativeUrlFormatter {

    private final ObjectMapper objectMapper;

    public CreativeUrlFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildCompanyCreativeUrl(AttentiveConfig attentiveConfig) {
        Uri.Builder uriBuilder =
                getCompanyCreativeUriBuilder(attentiveConfig.getDomain(), attentiveConfig.getMode());

        UserIdentifiers userIdentifiers = attentiveConfig.getUserIdentifiers();

        addUserIdentifiersAsParameters(uriBuilder, userIdentifiers);

        return uriBuilder.build().toString();
    }

    private Uri.Builder getCompanyCreativeUriBuilder(String domain, AttentiveConfig.Mode mode) {
        Uri.Builder creativeUriBuilder = new Uri.Builder()
                .scheme("https")
                .authority("creatives.attn.tv")
                .path("mobile-apps/index.html")
                .appendQueryParameter("domain", domain);


        if (mode == AttentiveConfig.Mode.DEBUG) {
            creativeUriBuilder.appendQueryParameter("debug", "matter-trip-grass-symbol");
        }

        creativeUriBuilder.appendQueryParameter("sdkVersion", AppInfo.getAttentiveSDKVersion());
        creativeUriBuilder.appendQueryParameter("sdkName", AppInfo.getAttentiveSDKName());

        return creativeUriBuilder;
    }

    private void addUserIdentifiersAsParameters(Uri.Builder builder,
                                                UserIdentifiers userIdentifiers) {
        if (userIdentifiers.getVisitorId() != null) {
            builder.appendQueryParameter("vid", userIdentifiers.getVisitorId());
        } else {
            Log.e(this.getClass().getName(), "No VisitorId found. This should not happen.");
        }

        if (userIdentifiers.getClientUserId() != null) {
            builder.appendQueryParameter("cuid", userIdentifiers.getClientUserId());
        }
        if (userIdentifiers.getPhone() != null) {
            builder.appendQueryParameter("p", userIdentifiers.getPhone());
        }
        if (userIdentifiers.getEmail() != null) {
            builder.appendQueryParameter("e", userIdentifiers.getEmail());
        }
        if (userIdentifiers.getKlaviyoId() != null) {
            builder.appendQueryParameter("kid", userIdentifiers.getKlaviyoId());
        }
        if (userIdentifiers.getShopifyId() != null) {
            builder.appendQueryParameter("sid", userIdentifiers.getShopifyId());
        }
        if (!userIdentifiers.getCustomIdentifiers().isEmpty()) {
            builder.appendQueryParameter("cstm", getCustomIdentifiersJson(userIdentifiers));
        }
    }

    private String getCustomIdentifiersJson(UserIdentifiers userIdentifiers) {
        try {
            return objectMapper.writeValueAsString(userIdentifiers.getCustomIdentifiers());
        } catch (JsonProcessingException e) {
            Log.e(this.getClass().getName(), "Could not serialize the custom identifiers. Message: " + e.getMessage());
            return "{}";
        }
    }
}
