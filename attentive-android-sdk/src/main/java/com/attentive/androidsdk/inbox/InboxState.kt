package com.attentive.androidsdk.inbox

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

/**
 * Represents the current state of the user's inbox.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Inbox is not yet available for public use.",
    level = DeprecationLevel.WARNING,
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class InboxState(
    val messages: List<Message> = emptyList(),
    val unreadCount: Int = messages.count { !it.isRead },
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val currentOffset: Int = 0,
)
