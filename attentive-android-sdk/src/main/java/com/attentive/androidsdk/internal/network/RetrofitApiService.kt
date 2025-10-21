package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.internal.network.events.BaseEventRequest
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
    ): Unit

    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json"
    )
    @POST("user-update")
    fun updateUser(
        @Body request: UserUpdateRequest
    ): Call<Unit>

    /**
     * Send analytics events to the Attentive Internal Events API
     * Supports all event types defined in the OpenAPI schema
     */
    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json"
    )
    @POST("mobile")
    suspend fun sendEvent(
        @Body request: BaseEventRequest
    ): Unit

    /**
     * Send analytics events to the Attentive Internal Events API (non-suspending version)
     * Supports all event types defined in the OpenAPI schema
     */
    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json"
    )
    @POST("mobile")
    fun sendEventCall(
        @Body request: BaseEventRequest
    ): Call<Unit>
}