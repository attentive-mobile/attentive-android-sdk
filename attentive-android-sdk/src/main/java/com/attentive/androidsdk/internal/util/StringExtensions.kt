package com.attentive.androidsdk.internal.util

import kotlinx.serialization.json.Json
import timber.log.Timber


fun String.toJsonEncodedString(): String {
    return try {
        Json.encodeToString(this).removeSurrounding("\"")
    } catch (e: Exception) {
        Timber.e(e, "Failed to convert object to JSON string")
        throw RuntimeException(e)
    }
}
