package com.attentive.androidsdk.internal.util;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.UserIdentifiers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CreativeUrlFormatter {

    @NonNull
    private final ObjectMapper objectMapper;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public CreativeUrlFormatter(@NonNull ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String buildCompanyCreativeUrl(@NonNull AttentiveConfig attentiveConfig, @Nullable String creativeId) {
        @NonNull
        final Uri.Builder uriBuilder = getCompanyCreativeUriBuilder(attentiveConfig, creativeId);

        @NonNull
        final UserIdentifiers userIdentifiers = attentiveConfig.getUserIdentifiers();

        addUserIdentifiersAsParameters(uriBuilder, userIdentifiers);

        return uriBuilder.build().toString();
    }

    @NonNull
    private Uri.Builder getCompanyCreativeUriBuilder(
            @NonNull AttentiveConfig config,
            @Nullable String creativeId
    ) {
        String domain = config.getDomain();
        AttentiveConfig.Mode mode = config.getMode();
        boolean skipFatigue = config.skipFatigueOnCreatives();

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
        creativeUriBuilder.appendQueryParameter("skipFatigue", Boolean.toString(skipFatigue));

        if (creativeId != null) {
            creativeUriBuilder.appendQueryParameter("attn_creative_id", creativeId);
        }

        return creativeUriBuilder;
    }

    private void addUserIdentifiersAsParameters(
            @NonNull Uri.Builder builder,
            @NonNull UserIdentifiers userIdentifiers
    ) {
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

    @NonNull
    private String getCustomIdentifiersJson(@NonNull UserIdentifiers userIdentifiers) {
        try {
            return objectMapper.writeValueAsString(userIdentifiers.getCustomIdentifiers());
        } catch (JsonProcessingException e) {
            Log.e(this.getClass().getName(), "Could not serialize the custom identifiers. Message: " + e.getMessage());
            return "{}";
        }
    }
}
