package com.attentive.androidsdk.inbox

import kotlinx.serialization.Serializable

/**
 * Represents the current state of the user's inbox.
 */
@Serializable
data class InboxState(
    val messages: List<Message> = emptyList(),
    val unreadCount: Int = messages.count { !it.isRead }
)
