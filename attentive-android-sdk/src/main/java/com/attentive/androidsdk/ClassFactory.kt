package com.attentive.androidsdk

import android.content.Context
import com.attentive.androidsdk.internal.network.UserAgentInterceptor
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Interceptor
import okhttp3.OkHttpClient

object ClassFactory {
    @JvmStatic
    fun buildPersistentStorage(context: Context): PersistentStorage {
        return PersistentStorage(context)
    }

    @JvmStatic
    fun buildVisitorService(persistentStorage: PersistentStorage): VisitorService {
        return VisitorService(persistentStorage)
    }

    @JvmStatic
    fun buildObjectMapper(): ObjectMapper {
        return ObjectMapper()
    }

    @JvmStatic
    fun buildOkHttpClient(interceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    fun buildUserAgentInterceptor(context: Context?): Interceptor {
        return UserAgentInterceptor(context)
    }

    @JvmStatic
    fun buildAttentiveApi(okHttpClient: OkHttpClient, objectMapper: ObjectMapper): AttentiveApi {
        return AttentiveApi(okHttpClient, objectMapper)
    }

    fun buildSettingsService(persistentStorage: PersistentStorage): SettingsService {
        return SettingsService(persistentStorage)
    }
}
