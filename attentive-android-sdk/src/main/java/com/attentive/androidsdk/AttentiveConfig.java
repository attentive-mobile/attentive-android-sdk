package com.attentive.androidsdk;

import android.util.Log;

import com.attentive.androidsdk.creatives.HttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttentiveConfig {

    public enum Mode {
        DEBUG,
        PRODUCTION
    }

    private final Mode mode;
    private final String domain;
    private UserIdentifiers userIdentifiers;
    private final ApiClass apiClass;

    public AttentiveConfig(String domain, Mode mode) {
        this.domain = domain;
        this.mode = mode;

        this.apiClass = new ApiClass();
    }

    public Mode getMode() {
        return mode;
    }

    public String getDomain() {
        return domain;
    }

    public String getAppUserId() { return userIdentifiers.getAppUserId(); }

    public UserIdentifiers getUserIdentifiers() {
        return userIdentifiers;
    }

    public void identify(String appUserId) {
        ParameterValidation.verifyNotEmpty(appUserId, "appUserId");

        identify(new UserIdentifiers.Builder(appUserId).build());
    }

    public void identify(UserIdentifiers userIdentifiers) {
        ParameterValidation.verifyNotNull(userIdentifiers, "userIdentifiers");

        this.userIdentifiers = userIdentifiers;
        callIdentifyApi(userIdentifiers);
    }

    private String extractDomain() {
        try {
            //URL url = new URL("https://events.dev.attentivemobile.com/e");
            URL url = new URL("https://cdn.dev.attn.tv/" + getDomain() + "/dtag.js");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            String fullTag = builder.toString();
            Pattern pattern = Pattern.compile("window.__attentive_domain='(.*?).dev.attn.tv'");
            Matcher matcher = pattern.matcher(fullTag);
            if (matcher.find()) {
                if (matcher.groupCount() == 1) {
                    // TODO check domain value
                    return matcher.group(1);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO return Optional instead
        return null;
    }

    private void identifyCall() {
        // get the actual domain
        // get identifiers ready
        // call the api with 1) identifiers, 2) domain
    }
    private void callIdentifyApi(UserIdentifiers userIdentifiers) {

    }

    private void callIdentifyApi2(UserIdentifiers userIdentifiers) {
        try {
            //URL url = new URL("https://events.dev.attentivemobile.com/e");
            String domain = extractDomain();
            URL url = new URL("https://events.dev.attentivemobile.com/e?v=4.16.10_f0149441bf&pd=https%3A%2F%2Feek-commerce.myshopify.com%2F&u=98a87b28597742bd9d58b26dabc40a01&c=" + domain + "&ceid=r5D&lt=1667318830497&tag=modern&cs=2919098594&t=idn&r=https%3A%2F%2Feek-commerce.myshopify.com%2F&cb=1667318830501&evs=%5B%7B%22vendor%22%3A%20%220%22%2C%20%22id%22%3A%20%22my-shopify-user-id7777%22%7D%2C%20%7B%22vendor%22%3A%20%221%22%2C%20%22id%22%3A%20%22my-klaviyo-user-id7777%22%7D%2C%20%7B%22vendor%22%3A%20%222%22%2C%20%22id%22%3A%20%22my-client-user-id7777%22%7D%2C%20%7B%22vendor%22%3A%20%226%22%2C%20%22name%22%3A%22my-custom-name%22%2C%20%22id%22%3A%20%22my-custom-id7777%22%7D%5D&m=%7B%22source%22%3A%22msdk%22%2C%22phone%22%3A%229731237777%22%2C%22email%22%3A%22usertest7777%40gmail.com%22%7D");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}