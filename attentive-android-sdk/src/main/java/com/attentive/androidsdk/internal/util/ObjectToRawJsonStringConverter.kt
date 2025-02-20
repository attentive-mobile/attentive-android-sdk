package com.attentive.androidsdk.internal.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class ObjectToRawJsonStringConverter {

    fun convert(value: Any?): String {
        return try {
            Json.encodeToString(value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert object to JSON string")
            throw RuntimeException(e)
        }
    }
}