package com.attentive.androidsdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttentiveApiClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AttentiveApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void collectUserIdentifiers(String domain, UserIdentifiers userIdentifiers) {
        try {
            String geoAdjustedDomain = getGeoAdjustedDomain(domain);

            List<ExternalVendorId> externalVendorIds = buildExternalVendorIds(userIdentifiers);
            String externalVendorIdsJson = objectMapper.writeValueAsString(externalVendorIds);

            Metadata metadata = buildMetadata(userIdentifiers);
            String metadataJson = objectMapper.writeValueAsString(metadata);

            String query =
                    // TODO what version for the tag?
                    "v=4.16.10_f0149441bf" +
                    //"&pd=https%3A%2F%2Feek-commerce.myshopify.com%2F" +
                    //"&u=98a87b28597742bd9d58b26dabc40a01" +
                    "&c=" + geoAdjustedDomain +
                    //"&ceid=r5D" +
                    //"&lt=1667318830497" +
                    //"&tag=modern" +
                    //"&cs=2919098594" +
                    "&t=idn" +
                    //"&r=https%3A%2F%2Feek-commerce.myshopify.com%2F" +
                    //"&cb=1667318830501" +
                    "&evs=" + encode(externalVendorIdsJson) +
                    "&m=" + encode(metadataJson);
            String urlString = "https://events.dev.attentivemobile.com/e?" + query;
            httpClient.post(new URL(urlString));
        } catch (MalformedURLException | JsonProcessingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String encode(String stringToEncode) throws UnsupportedEncodingException {
        return URLEncoder.encode(stringToEncode,StandardCharsets.UTF_8.toString());
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

    // Converts a domain to a geo-adjusted domain, if it exists. E.g. if the domain is "siren-denim" and the user is in Canada, this will return "siren-denim-ca"
    private String getGeoAdjustedDomain(String domain) {
        try {
            URL url = new URL("https://cdn.dev.attn.tv/" + domain + "/dtag.js");
            String fullTag = httpClient.get(url);
            Pattern pattern = Pattern.compile("window.__attentive_domain='(.*?).dev.attn.tv'");
            Matcher matcher = pattern.matcher(fullTag);
            if (matcher.find()) {
                if (matcher.groupCount() == 1) {
                    // TODO check domain value
                    return matcher.group(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO return Optional instead
        return null;
    }
}
