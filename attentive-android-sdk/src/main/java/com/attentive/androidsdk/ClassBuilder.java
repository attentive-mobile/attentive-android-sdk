package com.attentive.androidsdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;

// Poor man's injection. Allows for easier testing.
class ClassBuilder {
    public static OkHttpClient buildOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    public static AttentiveApi buildAttentiveApi(OkHttpClient okHttpClient, ObjectMapper mapper) {
        return new AttentiveApi(okHttpClient, mapper);
    }

    public static ObjectMapper buildObjectMapper() {
        return new ObjectMapper();
    }
}
