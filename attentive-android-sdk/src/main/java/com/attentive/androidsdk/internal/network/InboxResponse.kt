package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.inbox.Message

internal data class InboxResponse(
    val messages: List<Message>,
    val unreadCount: Int,
    val hasMoreMessages: Boolean,
    val nextOffset: Int?,
)
