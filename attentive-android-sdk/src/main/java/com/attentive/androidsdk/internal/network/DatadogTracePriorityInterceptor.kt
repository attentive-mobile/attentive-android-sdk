package com.attentive.androidsdk.internal.network

import androidx.annotation.RestrictTo
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY)
class DatadogTracePriorityInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain.request()
                .newBuilder()
                .header(HEADER, VALUE)
                .build()
        return chain.proceed(request)
    }

    companion object {
        const val HEADER = "x-datadog-sampling-priority"
        const val VALUE = "1"
    }
}
