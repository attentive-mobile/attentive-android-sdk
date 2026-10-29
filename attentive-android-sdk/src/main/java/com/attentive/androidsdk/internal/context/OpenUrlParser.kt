package com.attentive.androidsdk.internal.context

import java.net.URI
import java.net.URLDecoder

/** Decides how the app was opened from the link it was opened with. */
internal fun interface LaunchClassifier {
    fun classify(uri: URI): LaunchSource
}

/** Web links are App Links; any other scheme is a custom-scheme deep link. */
internal object DefaultLaunchClassifier : LaunchClassifier {
    override fun classify(uri: URI): LaunchSource =
        when (uri.scheme?.lowercase()) {
            "http", "https" -> LaunchSource.UNIVERSAL_LINK
            else -> LaunchSource.DEEP_LINK
        }
}

internal data class OpenedUrl(
    val source: LaunchSource,
    val utms: Map<UtmKey, String>,
    val landingUrl: String,
)

/**
 * Extracts what the context store keeps from an opening link: its launch source, its UTM
 * parameters and the link without its query string or fragment. Nothing else is kept.
 */
internal class OpenUrlParser(
    private val classifier: LaunchClassifier = DefaultLaunchClassifier,
) {
    /** Returns null when [url] is not an absolute URI. */
    fun parse(url: String): OpenedUrl? {
        val uri =
            try {
                URI(url.trim())
            } catch (e: Exception) {
                return null
            }
        val scheme = uri.scheme ?: return null
        return OpenedUrl(
            source = classifier.classify(uri),
            utms = utmParams(uri.rawQuery ?: uri.rawSchemeSpecificPart.substringAfter('?', "")),
            landingUrl = landingUrl(uri, scheme),
        )
    }

    private fun utmParams(rawQuery: String): Map<UtmKey, String> {
        val params = mutableMapOf<UtmKey, String>()
        for (pair in rawQuery.split('&')) {
            val name = decode(pair.substringBefore('=')) ?: continue
            val key = UtmKey.entries.firstOrNull { it.queryName == name } ?: continue
            val value = decode(pair.substringAfter('=', ""))?.trim()?.take(MAX_UTM_VALUE_LENGTH)
            if (!value.isNullOrEmpty()) {
                params[key] = value
            }
        }
        return params
    }

    private fun landingUrl(uri: URI, scheme: String): String =
        if (uri.isOpaque) {
            "$scheme:${uri.rawSchemeSpecificPart.substringBefore('?')}"
        } else {
            "$scheme:" + (uri.rawAuthority?.let { "//$it" } ?: "") + (uri.rawPath ?: "")
        }

    private fun decode(value: String): String? =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: IllegalArgumentException) {
            null
        }

    companion object {
        internal const val MAX_UTM_VALUE_LENGTH = 256
    }
}
