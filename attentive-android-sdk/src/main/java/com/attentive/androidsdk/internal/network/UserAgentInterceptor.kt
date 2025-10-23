package com.attentive.androidsdk.internal.network

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.attentive.androidsdk.internal.util.AppInfo
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class UserAgentInterceptor(context: Context?) : Interceptor {
    private val context = context!!

    // This adds the user agent header to every request that the OkHttpClient makes
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val requestWithUserAgentAdded = request.newBuilder().header(
            USER_AGENT_HEADER_NAME,
            userAgent
        ).build()
        return chain.proceed(requestWithUserAgentAdded)
    }

    @get:VisibleForTesting
    val userAgent: String
        get() {
            val appName = AppInfo.getApplicationName(context)
            val appNameEncoded = if (appName == null) {
                null
            } else {
                encodeForHeader(appName.replace(" ", "-"))
            }
            return String.format(
                "%s/%s (%s; Android %s; Android API Level %s) %s/%s",
                appNameEncoded,
                AppInfo.getApplicationVersion(context),
                AppInfo.getApplicationPackageName(context),
                AppInfo.androidVersion,
                AppInfo.androidLevel,
                AppInfo.attentiveSDKName,
                AppInfo.attentiveSDKVersion
            )
        }

    /**
     * Encodes a string to be safe for HTTP headers by URL-encoding non-ASCII characters.
     * This preserves the original information so the backend can decode it.
     * For example: "Béis" becomes "B%C3%A9is"
     */
    @VisibleForTesting
    internal fun encodeForHeader(input: String): String {
        // URL encode the entire string, then decode ASCII-safe characters back
        // This preserves non-ASCII characters as percent-encoded while keeping readable ASCII
        val encoded = URLEncoder.encode(input, StandardCharsets.UTF_8.toString())

        // URLEncoder also encodes some characters that are safe for headers (like dashes, dots)
        // so we decode those back for readability
        return encoded
            .replace("+", "%20")  // URLEncoder uses + for spaces, but %20 is clearer
            .replace("%2D", "-")  // dash
            .replace("%2E", ".")  // dot
            .replace("%5F", "_")  // underscore
    }

    companion object {
        var USER_AGENT_HEADER_NAME: String = "User-Agent"
    }
}
