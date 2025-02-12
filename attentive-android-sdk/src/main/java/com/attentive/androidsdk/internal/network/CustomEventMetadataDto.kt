package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.internal.util.ObjectToRawJsonStringConverter
import com.fasterxml.jackson.databind.annotation.JsonSerialize

class CustomEventMetadataDto : Metadata() {
    var type: String? = null

    /**
     * We expect this map of properties to be a string containing a valid json map rather than a List object
     * For example:
     * Instead of this: {"properties": { "Color": "Blue" }}
     * We want this:    {"properties": "{ \"Color\": \"Blue\" }" }
     */
    @JsonSerialize(converter = ObjectToRawJsonStringConverter::class)
    var properties: Map<String, String>? = null
}
