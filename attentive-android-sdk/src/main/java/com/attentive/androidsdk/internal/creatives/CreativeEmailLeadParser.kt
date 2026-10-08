package com.attentive.androidsdk.internal.creatives

import com.attentive.androidsdk.creatives.CreativeEmailLead
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

internal object CreativeEmailLeadParser {
    /** Parses an `EMAIL_LEAD` creative message. Returns `null` if [messageData] isn't a JSON object. */
    fun parse(messageData: String): CreativeEmailLead? {
        val json =
            try {
                Json.parseToJsonElement(messageData).jsonObject
            } catch (e: Exception) {
                Timber.e("Failed to parse EMAIL_LEAD message: %s", e.message)
                return null
            }
        return CreativeEmailLead(
            email = json.stringOrNull("email"),
            rawPayload = messageData,
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }
}
