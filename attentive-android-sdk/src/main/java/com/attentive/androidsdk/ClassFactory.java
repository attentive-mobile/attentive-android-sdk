package com.attentive.androidsdk;

import android.content.Context;
import com.attentive.androidsdk.internal.network.UserAgentInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
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

    public static OkHttpClient buildOkHttpClient(Interceptor interceptor) {
        return new OkHttpClient.Builder().addInterceptor(interceptor).build();
    }

    public static Interceptor buildUserAgentInterceptor(Context context) {
        return new UserAgentInterceptor(context);
    }

    public static AttentiveApi buildAttentiveApi(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        return new AttentiveApi(okHttpClient, objectMapper);
    }

    public static SettingsService buildSettingsService(PersistentStorage persistentStorage) {
        return new SettingsService(persistentStorage);
    }
}
