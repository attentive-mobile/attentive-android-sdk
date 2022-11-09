package com.attentive.androidsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class AttentiveApi {
    private static final String ATTENTIVE_EVENTS_ENDPOINT_HOST = "events.attentivemobile.com";
    private static final String ATTENTIVE_DTAG_URL = "https://cdn.attn.tv/%s/dtag.js";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AttentiveApi(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void sendUserIdentifiersCollectedEvent(String domain, UserIdentifiers userIdentifiers, AttentiveApiCallback callback) {
        // first get the geo-adjusted domain, and then call the events endpoint
        final String url = String.format(ATTENTIVE_DTAG_URL, domain);
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("Getting geo-adjusted domain failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() != 200) {
                    callback.onFailure(String.format("Getting geo-adjusted domain returned invalid code: '%d', message: '%s'", response.code(), response.message()));
                    return;
                }

                ResponseBody body = response.body();

                if (body == null) {
                    callback.onFailure("Getting geo-adjusted domain returned no body");
                    return;
                }

                String fullTag = response.body().string();
                String geoAdjustedDomain = parseAttentiveDomainFromTag(fullTag);

                if (geoAdjustedDomain == null) {
                    callback.onFailure("Could not parse the domain from the full tag");
                    return;
                }

                internalSendUserIdentifiersCollectedEventAsync(geoAdjustedDomain, userIdentifiers, callback);
            }
        });
    }

    private void internalSendUserIdentifiersCollectedEventAsync(String geoAdjustedDomain, UserIdentifiers userIdentifiers, AttentiveApiCallback callback) {
        String externalVendorIdsJson = null;
        try {
            List<ExternalVendorId> externalVendorIds = buildExternalVendorIds(userIdentifiers);
            externalVendorIdsJson = objectMapper.writeValueAsString(externalVendorIds);
        } catch (JsonProcessingException e) {
            callback.onFailure(String.format("Could not serialize the UserIdentifiers. Message: '%s'", e.getMessage()));
            return;
        }

        String metadataJson = null;
        try {
            Metadata metadata = buildMetadata(userIdentifiers);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            callback.onFailure(String.format("Could not serialize metadata. Message: '%s'", e.getMessage()));
            return;
        }

        HttpUrl url = getHttpUrlEventsEndpointBuilder()
                .addQueryParameter("v", "4.16.10_f0149441bf")
                .addQueryParameter("c", geoAdjustedDomain)
                .addQueryParameter("t", "idn")
                .addQueryParameter("evs", externalVendorIdsJson)
                .addQueryParameter("m", metadataJson)
                .build();

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(String.format("Error when calling the event endpoint: '%s'", e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    callback.onFailure(String.format("Invalid response code when calling the event endpoint: '%d', message: '%s'", response.code(), response.message()));
                    return;
                }

                callback.onSuccess();
            }
        });
    }

    @NonNull
    private HttpUrl.Builder getHttpUrlEventsEndpointBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host(ATTENTIVE_EVENTS_ENDPOINT_HOST)
                .addPathSegment("e");
    }

    @Nullable
    private String parseAttentiveDomainFromTag(String tag) {
        Pattern pattern = Pattern.compile("window.__attentive_domain='(.*?).attn.tv'");
        Matcher matcher = pattern.matcher(tag);
        if (matcher.find()) {
            if (matcher.groupCount() == 1) {
                return matcher.group(1);
            }
        }

        return null;
    }

    private Metadata buildMetadata(UserIdentifiers userIdentifiers) {
        Metadata metadata = new Metadata();
        metadata.setSource("msdk");

        if (userIdentifiers.getPhone() != null) {
            metadata.setPhone(userIdentifiers.getPhone());
        }

        if (userIdentifiers.getEmail() != null) {
            metadata.setEmail(userIdentifiers.getEmail());
        }

        return metadata;
    }

    private List<ExternalVendorId> buildExternalVendorIds(UserIdentifiers userIdentifiers) {
        List<ExternalVendorId> externalVendorIdList = new ArrayList<>();
        externalVendorIdList.add(
                new ExternalVendorId() {{
                    setVendor(Vendor.CLIENT_USER);
                    setId(userIdentifiers.getAppUserId());
                }});

        if (userIdentifiers.getShopifyId() != null) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(Vendor.SHOPIFY);
                setId(userIdentifiers.getShopifyId());
            }});
        }

        if (userIdentifiers.getKlaviyoId() != null) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(Vendor.KLAVIYO);
                setId(userIdentifiers.getKlaviyoId());
            }});
        }

        for (Map.Entry<String, String> customIdentifier : userIdentifiers.getCustomIdentifiers().entrySet()) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(Vendor.CUSTOM_USER);
                setId(customIdentifier.getKey());
                setName(customIdentifier.getValue());
            }});
        }

        return externalVendorIdList;
    }
}
