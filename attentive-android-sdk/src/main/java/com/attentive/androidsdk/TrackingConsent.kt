package com.attentive.androidsdk

/**
 * A shopper's choice about email open-tracking pixels, for the France and Italy email privacy
 * rules. Unrelated to App Tracking Transparency or the advertising ID.
 *
 * Pass it to [AttentiveSdk.optUserIntoMarketingSubscription] or
 * [AttentiveSdk.optUserOutOfMarketingSubscription] when your app has collected a choice. Your app
 * owns the UI; the SDK neither prompts nor stores. Don't infer a value — if the shopper hasn't
 * been asked, leave it [UNSPECIFIED].
 *
 * See the
 * [France email privacy compliance update](https://help.attentive.com/hc/en-us/articles/51464632390804-France-Email-Privacy-Compliance-Update).
 */
enum class TrackingConsent {
    /** No choice recorded. Sends no consent field, leaving the backend's locale defaulting. */
    UNSPECIFIED,

    /** The shopper agreed to email open-tracking. */
    ACCEPTED,

    /** The shopper declined email open-tracking. */
    DECLINED,
}

/**
 * The wire value, or null to omit the field.
 *
 * [TrackingConsent.UNSPECIFIED] maps to null rather than `"UNSPECIFIED"`: an absent field tells
 * the backend to apply its own locale defaulting. Gson drops null properties, so null is what
 * keeps the key out of the body.
 */
internal fun TrackingConsent.toWireValue(): String? =
    when (this) {
        TrackingConsent.UNSPECIFIED -> null
        TrackingConsent.ACCEPTED, TrackingConsent.DECLINED -> name
    }
