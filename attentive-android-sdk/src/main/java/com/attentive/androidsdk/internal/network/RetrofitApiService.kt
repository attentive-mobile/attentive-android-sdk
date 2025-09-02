package com.attentive.androidsdk.internal.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitApiService {
    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json"
    )

    @POST("user-update")
    suspend fun updateUserSuspend(
        @Body request: UserUpdateRequest
    ): Unit // or a concrete response model if the API returns JSON

    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json"
    )

    @POST("user-update")
    fun updateUser(
        @Body request: UserUpdateRequest
    ): Call<Unit>
}