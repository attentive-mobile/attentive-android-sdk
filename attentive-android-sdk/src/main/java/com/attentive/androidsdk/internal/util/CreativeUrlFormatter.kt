package com.attentive.androidsdk.internal.util

import android.net.Uri
import androidx.annotation.RestrictTo
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveConfigInterface
import com.attentive.androidsdk.UserIdentifiers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@RestrictTo(RestrictTo.Scope.LIBRARY)
class CreativeUrlFormatter @RestrictTo(RestrictTo.Scope.LIBRARY) constructor() {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun buildCompanyCreativeUrl(attentiveConfig: AttentiveConfigInterface, creativeId: String?): String {
        val uriBuilder = getCompanyCreativeUriBuilder(attentiveConfig, creativeId)

        val userIdentifiers = attentiveConfig.userIdentifiers

        addUserIdentifiersAsParameters(uriBuilder, userIdentifiers)

        return uriBuilder.build().toString()
    }

    private fun getCompanyCreativeUriBuilder(
        config: AttentiveConfigInterface,
        creativeId: String?
    ): Uri.Builder {
        val domain = config.domain
        val mode = config.mode
        val skipFatigue = config.skipFatigueOnCreatives()

        val creativeUriBuilder = Uri.Builder()
            .scheme("https")
            .authority("creatives.attn.tv")
            .path("mobile-apps/index.html")
            .appendQueryParameter("domain", domain)

        if (mode == AttentiveConfig.Mode.DEBUG) {
            creativeUriBuilder.appendQueryParameter("debug", "matter-trip-grass-symbol")
        }

        creativeUriBuilder.appendQueryParameter("sdkVersion", AppInfo.attentiveSDKVersion)
        creativeUriBuilder.appendQueryParameter("sdkName", AppInfo.attentiveSDKName)
        creativeUriBuilder.appendQueryParameter("skipFatigue", skipFatigue.toString())

        if (creativeId != null) {
            creativeUriBuilder.appendQueryParameter("attn_creative_id", creativeId)
        }

        return creativeUriBuilder
    }

    private fun addUserIdentifiersAsParameters(
        builder: Uri.Builder,
        userIdentifiers: UserIdentifiers
    ) {
        userIdentifiers.visitorId?.let {
            builder.appendQueryParameter("vid", it)
        } ?: run {
            Timber.e("No VisitorId found. This should not happen.")
        }

        userIdentifiers.clientUserId?.let { builder.appendQueryParameter("cuid", it) }
        userIdentifiers.phone?.let { builder.appendQueryParameter("p", it) }
        userIdentifiers.email?.let { builder.appendQueryParameter("e", it) }
        userIdentifiers.klaviyoId?.let { builder.appendQueryParameter("kid", it) }
        userIdentifiers.shopifyId?.let { builder.appendQueryParameter("sid", it) }

        if (userIdentifiers.customIdentifiers.isNotEmpty()) {
            builder.appendQueryParameter("cstm", getCustomIdentifiersJson(userIdentifiers))
        }
    }

    private fun getCustomIdentifiersJson(userIdentifiers: UserIdentifiers): String {
        return runCatching {
            Json.encodeToString(userIdentifiers.customIdentifiers)
        }.getOrElse { e ->
            Timber.e("Could not serialize the custom identifiers. Message: %s", e.message)
            "{}"
        }
    }
}