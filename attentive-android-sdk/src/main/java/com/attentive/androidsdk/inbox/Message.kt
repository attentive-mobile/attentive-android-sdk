package com.attentive.androidsdk.inbox

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

/**
 * Enum representing the display style of a message.
 */
@Deprecated(
    message = "Inbox is not yet available for public use.",
    level = DeprecationLevel.WARNING,
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
enum class Style(val value: String) {
    Small("small"),
    Large("large"),
}

/**
 * Represents a single message in the inbox.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Inbox is not yet available for public use.",
    level = DeprecationLevel.WARNING,
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class Message(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val imageUrl: String? = null,
    val actionUrl: String? = null,
    val style: Style = Style.Small,
)
