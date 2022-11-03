package com.attentive.androidsdk;

import com.attentive.androidsdk.creatives.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiClient {
    private final HttpClient httpClient;

    public ApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void collectUserIdentifiers(String domain, UserIdentifiers userIdentifiers) {
        try {
            String geoAdjustedDomain = getGeoAdjustedDomain(domain);

            JSONArray externalVendorIds = constructUserIdentifierArray(userIdentifiers);

            JSONObject metadata = buildMetadata(userIdentifiers);

            String query =
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
                    "&evs=" + externalVendorIds.toString() +
                    "&m=" + metadata.toString();
            URI uri = new URI("https", null, "events.dev.attentivemobile.com", -1, "e", query, null);
            String returnBody = httpClient.post(uri.toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private JSONObject buildMetadata(UserIdentifiers userIdentifiers) {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put("source", "msdk");

            if (userIdentifiers.getEmail() != null) {
                metadata.put("email", userIdentifiers.getEmail());
            }

            if (userIdentifiers.getPhone() != null) {
                metadata.put("phone", userIdentifiers.getPhone());
            }

            return metadata;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return metadata;
    }

    private JSONArray constructUserIdentifierArray(UserIdentifiers userIdentifiers) {
        JSONArray identifiers = new JSONArray();
        try {
            JSONObject appUserId = new JSONObject();
            appUserId.put("id", userIdentifiers.getAppUserId());
            appUserId.put("vendor", VendorIdentifierValue.CLIENT_USER);
            identifiers.put(appUserId);

            if (userIdentifiers.getKlaviyoId() != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", userIdentifiers.getKlaviyoId());
                jsonObject.put("vendor", VendorIdentifierValue.KLAVIYO);
                identifiers.put(jsonObject);
            }
            if (userIdentifiers.getShopifyId() != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", userIdentifiers.getShopifyId());
                jsonObject.put("vendor", VendorIdentifierValue.SHOPIFY);
                identifiers.put(jsonObject);
            }
            if (userIdentifiers.getCustomIdentifiers() != null) {
                for (UserIdentifiers.CustomIdentifier customIdentifier : userIdentifiers.getCustomIdentifiers()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", customIdentifier.getValue());
                    jsonObject.put("name", customIdentifier.getName());
                    jsonObject.put("vendor", VendorIdentifierValue.CUSTOM_USER);
                    identifiers.put(jsonObject);
                }
            }

            return identifiers;
        } catch (JSONException e) {
            // TODO wyatt
            e.printStackTrace();
        }

        return new JSONArray();
    }

    // Converts a domain to a geo-adjusted domain, if it exists. E.g. if the domain is "siren-denim" and the user is in Canada, this will return "siren-denim-ca"
    private String getGeoAdjustedDomain(String domain) {
        try {
            //URL url = new URL("https://events.dev.attentivemobile.com/e");
            URL url = new URL("https://cdn.dev.attn.tv/" + domain + "/dtag.js");
            String responseBody = httpClient.get(url);
            String fullTag = responseBody;
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
