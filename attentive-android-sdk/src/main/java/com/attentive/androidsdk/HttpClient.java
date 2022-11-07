package com.attentive.androidsdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class HttpClient {
    public String post(URL url) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            // a call to "getResponseCode" makes the HTTP call
            int responseCode = urlConnection.getResponseCode();
            // TODO what to do with the response code? Throw exception if not success?
            return readBodyFromURLConnection(urlConnection);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String get(URL url) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            // a call to "getResponseCode" makes the HTTP call
            int responseCode = urlConnection.getResponseCode();
            // TODO what to do with the response code? Throw exception if not success?
            return readBodyFromURLConnection(urlConnection);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readBodyFromURLConnection(URLConnection urlConnection) throws IOException {
        InputStream inputStream = urlConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            builder.append(line);
        }

        return builder.toString();
    }
}
