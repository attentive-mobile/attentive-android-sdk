package com.attentive.androidsdk;

import android.content.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

public class ClassFactory {
    public static PersistentStorage buildPersistentStorage(Context context) {
        return new PersistentStorage(context);
    }

    public static VisitorService buildVisitorService(PersistentStorage persistentStorage) {
        return new VisitorService(persistentStorage);
    }

    public static ObjectMapper buildObjectMapper() {
        return new ObjectMapper();
    }

    public static OkHttpClient buildOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    public static AttentiveApi buildAttentiveApi(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        return new AttentiveApi(okHttpClient, objectMapper);
    }
}
