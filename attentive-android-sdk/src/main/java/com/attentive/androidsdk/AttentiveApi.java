package com.attentive.androidsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// TODO name
class AttentiveApi {
    private static final String ATTENTIVE_EVENTS_ENDPOINT_HOST = "events.dev.attentivemobile.com";

    private final Executor executor;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AttentiveApi(Executor executor, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.executor = executor;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void callIdentifyAsync(String domain, UserIdentifiers userIdentifiers) {
        executor.execute(() -> callIdentifySynchronously(domain, userIdentifiers));
    }

    private void callIdentifySynchronously(String domain, UserIdentifiers userIdentifiers) {
        String geoAdjustedDomain = getGeoAdjustedDomain(domain);

        if (geoAdjustedDomain == null) {
            // TODO handle
            return;
        }

        String externalVendorIdsJson = null;
        String metadataJson = null;
        try {
            List<ExternalVendorId> externalVendorIds = buildExternalVendorIds(userIdentifiers);
            externalVendorIdsJson = objectMapper.writeValueAsString(externalVendorIds);

            Metadata metadata = buildMetadata(userIdentifiers);
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        HttpUrl url = getHttpUrlEventsEndpointBuilder()
                .addQueryParameter("v", "4.16.10_f0149441bf")
                .addQueryParameter("c", geoAdjustedDomain)
                .addQueryParameter("t", "idn")
                .addQueryParameter("evs", externalVendorIdsJson)
                .addQueryParameter("m", metadataJson)
                .build();

        Request request = new Request.Builder().url(url).build();
        try {
            Response response = httpClient.newCall(request).execute();
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private HttpUrl.Builder getHttpUrlEventsEndpointBuilder() {
        return new HttpUrl.Builder()
                .scheme("https")
                .host(ATTENTIVE_EVENTS_ENDPOINT_HOST)
                .addPathSegment("e");
    }

    private String encode(String stringToEncode) throws UnsupportedEncodingException {
        return URLEncoder.encode(stringToEncode, StandardCharsets.UTF_8.toString());
    }

    @Nullable
    private String getGeoAdjustedDomain(String domain) {
        try {
            final String url = String.format("https://cdn.dev.attn.tv/%s/dtag.js", domain);
            Request request = new Request.Builder().url(url).build();
            Response response = httpClient.newCall(request).execute();

            // We want to check for 200 specifically instead of just "success" because this endpoint can return other success response codes, like 204 if there is no dtag
            if (response.code() != 200) {
                return null;
            }

            String fullTag = response.body().string();
            return parseAttentiveDomainFromTag(fullTag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    private String parseAttentiveDomainFromTag(String tag) {
        Pattern pattern = Pattern.compile("window.__attentive_domain='(.*?).dev.attn.tv'");
        Matcher matcher = pattern.matcher(tag);
        if (matcher.find()) {
            if (matcher.groupCount() == 1) {
                // TODO check domain value
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
                    setVendor(VendorIdentifierValue.CLIENT_USER);
                    setId(userIdentifiers.getAppUserId());
                }});

        if (userIdentifiers.getShopifyId() != null) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(VendorIdentifierValue.SHOPIFY);
                setId(userIdentifiers.getShopifyId());
            }});
        }

        if (userIdentifiers.getKlaviyoId() != null) {
            externalVendorIdList.add(new ExternalVendorId() {{
                setVendor(VendorIdentifierValue.KLAVIYO);
                setId(userIdentifiers.getKlaviyoId());
            }});
        }

        if (userIdentifiers.getCustomIdentifiers() != null) {
            for (UserIdentifiers.CustomIdentifier customIdentifier : userIdentifiers.getCustomIdentifiers()) {
                externalVendorIdList.add(new ExternalVendorId() {{
                    setVendor(VendorIdentifierValue.CUSTOM_USER);
                    setId(customIdentifier.getValue());
                    setName(customIdentifier.getName());
                }});
            }
        }

        return externalVendorIdList;
    }
}
