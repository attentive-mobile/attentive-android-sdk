package com.attentive.androidsdk.inbox

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

/**
 * Enum representing the display style of a message.
 */

@Serializable
enum class Style(val value: String) {
    Small("small"),
    Large("large"),
}


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
