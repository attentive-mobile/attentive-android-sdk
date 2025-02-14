package com.attentive.androidsdk.internal.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.StdConverter
import timber.log.Timber

class ObjectToRawJsonStringConverter : StdConverter<Any?, String>() {
    private val objectMapper = ObjectMapper()

    override fun convert(value: Any?): String {
        try {
            return objectMapper.writeValueAsString(value)
        } catch (e: JsonProcessingException) {
            Timber.e(e, "Failed to convert object to JSON string")
            throw RuntimeException(e)
        }
    }
}