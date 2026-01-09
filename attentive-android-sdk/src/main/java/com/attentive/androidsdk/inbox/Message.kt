package com.attentive.androidsdk.inbox

import kotlinx.serialization.Serializable

/**
 * Represents a single message in the inbox.
 */
@Serializable
data class Message(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val imageUrl: String? = null,
    val actionUrl: String? = null
)
