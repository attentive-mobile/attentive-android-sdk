package com.attentive.androidsdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;

// Poor man's injection. Allows for easier testing.
class ClassFactory {
    public static OkHttpClient createOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    public static AttentiveApi createAttentiveApi(OkHttpClient okHttpClient, ObjectMapper mapper) {
        return new AttentiveApi(okHttpClient, mapper);
    }

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }
}
