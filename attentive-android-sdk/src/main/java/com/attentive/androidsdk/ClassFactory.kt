package com.attentive.androidsdk

import android.content.Context
import com.attentive.androidsdk.internal.network.UserAgentInterceptor
import com.attentive.androidsdk.internal.util.AppInfo
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import kotlin.math.log


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
    fun buildOkHttpClient(logLevel: AttentiveLogLevel?, interceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        if(logLevel == AttentiveLogLevel.VERBOSE){
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        } else if(logLevel == AttentiveLogLevel.STANDARD){
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        } else if(logLevel == AttentiveLogLevel.LIGHT){
            logging.setLevel(HttpLoggingInterceptor.Level.NONE)
        }
        return OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(logging).build()
    }

    fun buildUserAgentInterceptor(context: Context?): Interceptor {
        return UserAgentInterceptor(context)
    }

    @JvmStatic
    fun buildAttentiveApi(okHttpClient: OkHttpClient, domain: String): AttentiveApi {
        return AttentiveApi(okHttpClient, domain)
    }

    fun buildSettingsService(persistentStorage: PersistentStorage): SettingsService {
        return SettingsService(persistentStorage)
    }
}
