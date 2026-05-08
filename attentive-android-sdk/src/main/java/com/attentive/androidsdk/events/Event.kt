package com.attentive.androidsdk.events

import kotlinx.serialization.Serializable

/**
 * Base class for all events recorded via [com.attentive.androidsdk.AttentiveSdk.recordEvent].
 *
 * Concrete subclasses: [PurchaseEvent], [AddToCartEvent], [ProductViewEvent], [CustomEvent].
 */
@Serializable
abstract class Event
