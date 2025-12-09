package com.attentive.androidsdk.internal.network

import com.attentive.androidsdk.internal.network.events.BaseEventRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitApiService {
    @Headers(
        "x-datadog-sampling-priority: 1", "Content-Type: application/json"
    )
    @POST("user-update")
    fun updateUser(
        @Body request: UserUpdateRequest
    ): Call<Unit>


    @Headers("x-datadog-sampling-priority: 1")
    @FormUrlEncoded
    @POST("mobile")
    fun sendEvent(
        @Field("d") eventData: String
    ): Call<Unit>
}