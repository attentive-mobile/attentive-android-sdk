package com.attentive.androidsdk.internal.network

import com.google.gson.annotations.SerializedName

internal data class InboxResponse(
    @SerializedName("messages")
    val messages: List<InboxMessageDto> = emptyList(),
    @SerializedName("next_page_token")
    val nextPageToken: String? = null,
)

internal data class InboxMessageDto(
    @SerializedName("inbox_message_id")
    val inboxMessageId: String,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("body")
    val body: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("action_url")
    val actionUrl: String? = null,
    @SerializedName("sent_at")
    val sentAt: String? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    @SerializedName("is_read")
    val isRead: Boolean = false,
    @SerializedName("read_at")
    val readAt: String? = null,
)
