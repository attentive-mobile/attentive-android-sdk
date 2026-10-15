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
        val json = parseObject(messageData) ?: return null
        return json.toLead(messageData)
    }

    /**
     * Parses a `LEAD` creative message. Returns `null` unless it carries an email that differs from
     * the `userEmail` the creative already had, so a `LEAD` that only repeats a known email is skipped.
     */
    fun parseLead(messageData: String): CreativeEmailLead? {
        val json = parseObject(messageData) ?: return null
        val email = json.stringOrNull("email") ?: return null
        if (email.equals(json.stringOrNull("userEmail"), ignoreCase = true)) return null
        return json.toLead(messageData)
    }

    private fun parseObject(messageData: String): JsonObject? =
        try {
            Json.parseToJsonElement(messageData).jsonObject
        } catch (e: Exception) {
            Timber.e("Failed to parse lead message: %s", e.message)
            null
        }

    private fun JsonObject.toLead(messageData: String) =
        CreativeEmailLead(
            email = stringOrNull("email"),
            rawPayload =
                if (Creative.MESSAGE_ORIGIN_KEY in this) {
                    JsonObject(this - Creative.MESSAGE_ORIGIN_KEY).toString()
                } else {
                    messageData
                },
        )

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }
}
