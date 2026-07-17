package com.attentive.androidsdk.internal.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Url

internal interface RetrofitInboxApiService {
    @POST
    suspend fun getMessages(
        @Url url: String,
        @Body body: GetMessagesRequest,
    ): InboxResponse

    @POST
    suspend fun getUnreadCount(
        @Url url: String,
        @Body body: UnreadCountRequest,
    ): UnreadCountResponse

    @PATCH
    suspend fun markMessagesRead(
        @Url url: String,
        @Body body: MarkMessagesReadRequest,
    ): MarkMessagesReadResponse

    @PATCH
    suspend fun markMessagesUnread(
        @Url url: String,
        @Body body: MarkMessagesReadRequest,
    ): MarkMessagesReadResponse

    @HTTP(method = "DELETE", hasBody = true)
    suspend fun deleteMessage(
        @Url url: String,
        @Body body: DeleteMessageRequest,
    ): Response<DeleteMessageResponse>

    @POST
    suspend fun trackClick(
        @Url url: String,
        @Body body: TrackClickRequest,
    ): Response<Unit>
}
