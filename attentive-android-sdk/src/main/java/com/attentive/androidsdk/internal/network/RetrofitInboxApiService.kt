package com.attentive.androidsdk.internal.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

internal interface RetrofitInboxApiService {
    @GET("inbox/messages")
    suspend fun getMessages(
        @Query("c") domain: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): InboxResponse

    @PATCH("inbox/messages/{id}")
    suspend fun updateMessage(
        @Path("id") id: String,
        @Body body: UpdateMessageRequest,
    ): Response<Unit>

    @DELETE("inbox/messages/{id}")
    suspend fun deleteMessage(
        @Path("id") id: String,
        @Query("c") domain: String,
    ): Response<Unit>

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
}

@Serializable
internal data class UpdateMessageRequest(
    @SerialName("c") val domain: String,
    val isRead: Boolean,
)
