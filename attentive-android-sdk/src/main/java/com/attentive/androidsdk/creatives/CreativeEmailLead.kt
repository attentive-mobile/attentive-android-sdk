package com.attentive.androidsdk.creatives

/**
 * Emitted on [com.attentive.androidsdk.AttentiveSdk.creativeEmailLeads] when a user submits
 * their email address in a [Creative].
 *
 * @property email The submitted email address, or `null` if the creative didn't include one.
 * @property rawPayload The full JSON message the creative posted, for fields not modeled here. Its
 *   `action` is `EMAIL_LEAD`, or `LEAD` when the email came from a page that also asks for a
 *   phone number.
 */
data class CreativeEmailLead(
    val email: String?,
    val rawPayload: String,
)
