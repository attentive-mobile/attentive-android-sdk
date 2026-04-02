package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.AttentiveApi.Companion.ATTENTIVE_DTAG_URL
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.regex.Pattern

class GeoAdjustedDomainInterceptor(
    private val httpClient: OkHttpClient,
    private val domain: String,
) : Interceptor {
    @Volatile
    private var cachedGeoAdjustedDomain: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val geoDomain = getGeoAdjustedDomain()
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        var newRequestBuilder =
            originalRequest.newBuilder()
                .url(originalUrl)

        // Only modify body for POST requests with a body
        if (originalRequest.method == "POST" && originalRequest.body != null) {
            val buffer = okio.Buffer()
            originalRequest.body!!.writeTo(buffer)
            var bodyString = buffer.readUtf8()

            // Replace the c= field (assuming format: c=oldvalue)
            bodyString = bodyString.replace(Regex("c=[^&]*"), "c=$geoDomain")

            val newBody = RequestBody.create(originalRequest.body!!.contentType(), bodyString)
            newRequestBuilder = newRequestBuilder.method(originalRequest.method, newBody)
        }

        return chain.proceed(newRequestBuilder.build())
    }

    private fun getGeoAdjustedDomain(): String {
        cachedGeoAdjustedDomain?.let { return it }

        val url = String.format(ATTENTIVE_DTAG_URL, domain)
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()

        if (response.code != 200 || response.body == null) {
            throw IOException("Failed to get geo-adjusted domain")
        }

        val fullTag = response.body!!.string()
        val geoAdjustedDomain =
            parseAttentiveDomainFromTag(fullTag)
                ?: throw IOException("Could not parse the domain from the full tag")

        cachedGeoAdjustedDomain = geoAdjustedDomain
        return geoAdjustedDomain
    }

    private fun parseAttentiveDomainFromTag(tag: String): String? {
        val pattern = Pattern.compile("='([a-z0-9-]+)[.]attn[.]tv'")
        val matcher = pattern.matcher(tag)
        if (matcher.find()) {
            if (matcher.groupCount() == 1) {
                return matcher.group(1)
            }
        }

        return null
    }
}
