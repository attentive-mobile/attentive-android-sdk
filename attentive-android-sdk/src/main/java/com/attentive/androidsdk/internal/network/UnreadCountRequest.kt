package com.attentive.androidsdk.internal.network

import com.google.gson.annotations.SerializedName

internal data class UnreadCountRequest(
    @SerializedName("visitor_id")
    val visitorId: String,
    @SerializedName("push_token")
    val pushToken: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
)

internal data class UnreadCountResponse(
    @SerializedName("unread_count")
    val unreadCount: Int,
)

internal data class GetMessagesRequest(
    @SerializedName("visitor_id")
    val visitorId: String,
    @SerializedName("push_token")
    val pushToken: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("page_token")
    val pageToken: String? = null,
    @SerializedName("since")
    val since: String? = null,
)

internal data class MarkMessagesReadRequest(
    @SerializedName("visitor_id")
    val visitorId: String,
    @SerializedName("push_token")
    val pushToken: String? = null,
    @SerializedName("message_ids")
    val messageIds: List<String>,
)

internal data class MarkMessagesReadResponse(
    @SerializedName("messages")
    val messages: List<MarkMessagesReadEntry> = emptyList(),
    @SerializedName("unread_count")
    val unreadCount: Int? = null,
)

internal data class MarkMessagesReadEntry(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("is_read")
    val isRead: Boolean,
)

internal data class DeleteMessageRequest(
    @SerializedName("visitor_id")
    val visitorId: String,
    @SerializedName("push_token")
    val pushToken: String? = null,
)

internal data class DeleteMessageResponse(
    @SerializedName("message_id")
    val messageId: String,
)

internal data class TrackClickRequest(
    @SerializedName("visitor_id")
    val visitorId: String,
    @SerializedName("push_token")
    val pushToken: String? = null,
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("action_url")
    val actionUrl: String? = null,
)
