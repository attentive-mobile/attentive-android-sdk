package com.attentive.androidsdk.internal.network

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.attentive.androidsdk.internal.util.AppInfo
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

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
            val appNameWithDashes =
                if (AppInfo.getApplicationName(context) == null) null else AppInfo.getApplicationName(
                    context
                )!!.replace(" ", "-")
            return String.format(
                "%s/%s (%s; Android %s; Android API Level %s) %s/%s",
                appNameWithDashes,
                AppInfo.getApplicationVersion(context),
                AppInfo.getApplicationPackageName(context),
                AppInfo.androidVersion,
                AppInfo.androidLevel,
                AppInfo.attentiveSDKName,
                AppInfo.attentiveSDKVersion
            )
        }

    companion object {
        var USER_AGENT_HEADER_NAME: String = "User-Agent"
    }
}
