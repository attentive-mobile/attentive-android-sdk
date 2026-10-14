package com.attentive.androidsdk.internal.creatives

import com.attentive.androidsdk.creatives.Creative
import com.attentive.androidsdk.creatives.CreativeEmailLead
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

internal object CreativeEmailLeadParser {
    /**
     * Parses an `EMAIL_LEAD` creative message. Returns `null` if [messageData] isn't a JSON object.
     * The origin the SDK adds while forwarding is removed from [CreativeEmailLead.rawPayload].
     */
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
            rawPayload =
                if (Creative.MESSAGE_ORIGIN_KEY in json) {
                    JsonObject(json - Creative.MESSAGE_ORIGIN_KEY).toString()
                } else {
                    messageData
                },
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }
}
