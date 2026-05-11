package com.attentive.androidsdk.internal.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

internal interface RetrofitInboxApiService {
    @GET("inbox/messages")
    suspend fun getMessages(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): InboxResponse

    @PATCH("inbox/messages/{id}")
    suspend fun updateMessage(
        @Path("id") id: String,
        @Body body: Map<String, Boolean>,
    ): Response<Unit>

    @DELETE("inbox/messages/{id}")
    suspend fun deleteMessage(
        @Path("id") id: String,
    ): Response<Unit>
}
