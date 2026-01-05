package com.attentive.androidsdk

import android.content.Context
import com.attentive.androidsdk.internal.network.AttentiveHttpLogger
import com.attentive.androidsdk.internal.network.GeoAdjustedDomainInterceptor
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
        val logging = HttpLoggingInterceptor(AttentiveHttpLogger())
        if(logLevel == AttentiveLogLevel.VERBOSE){
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        } else if(logLevel == AttentiveLogLevel.STANDARD){
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        }
        return OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(logging).build()
    }

    fun buildUserAgentInterceptor(context: Context?): Interceptor {
        return UserAgentInterceptor(context)
    }

    @JvmStatic
    internal fun buildAttentiveApi(okHttpClient: OkHttpClient, domain: String): AttentiveApi {
        val client = okHttpClient.newBuilder().addInterceptor(GeoAdjustedDomainInterceptor(okHttpClient, domain)).build()
        return AttentiveApi(client, domain)
    }

    fun buildSettingsService(persistentStorage: PersistentStorage): SettingsService {
        return SettingsService(persistentStorage)
    }
}
