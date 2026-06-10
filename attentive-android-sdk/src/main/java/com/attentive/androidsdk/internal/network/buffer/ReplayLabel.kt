package com.attentive.androidsdk.internal.network.buffer

import androidx.annotation.RestrictTo
import okhttp3.HttpUrl

/**
 * Maps a replayed URL to a human-readable description that mirrors the live-call
 * log lines from [com.attentive.androidsdk.AttentiveApi]. Used so log output during
 * a buffer flush is recognizable per event type rather than just opaque OkHttp traces.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object ReplayLabel {
    fun of(url: HttpUrl): String {
        val path = url.encodedPath
        return when {
            // Events: /e and /mobile carry an event-type abbreviation in the `t=` query param.
            path.endsWith("/e") || path.endsWith("/mobile") -> {
                val type = url.queryParameter("t")
                "${labelForEventType(type)} event"
            }
            path.endsWith("/token") -> "Push token registration"
            path.endsWith("/mtctrl") -> "Direct open status"
            path.endsWith("/user-update") -> "User update"
            path.endsWith("/opt-in-subscriptions") -> "Opt-in subscription"
            path.endsWith("/opt-out-subscriptions") -> "Opt-out subscription"
            else -> path
        }
    }

    private fun labelForEventType(abbreviation: String?): String =
        when (abbreviation) {
            "p" -> "PURCHASE"
            "oc" -> "ORDER_CONFIRMED"
            "d" -> "PRODUCT_VIEW"
            "c" -> "ADD_TO_CART"
            "i" -> "INFO"
            "ce" -> "CUSTOM_EVENT"
            "idn" -> "USER_IDENTIFIER_COLLECTED"
            null -> "UNKNOWN"
            else -> abbreviation.uppercase()
        }
}
