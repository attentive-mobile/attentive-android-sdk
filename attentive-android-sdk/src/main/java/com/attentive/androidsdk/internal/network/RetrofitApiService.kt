package com.attentive.androidsdk.internal.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitApiService {
    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json",
    )
    @POST("user-update")
    fun updateUser(
        @Body request: UserUpdateRequest,
    ): Call<Unit>

    @Headers("x-datadog-sampling-priority: 1")
    @FormUrlEncoded
    @POST("mobile")
    fun sendEvent(
        @Field("d") eventData: String,
    ): Call<Unit>

    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json",
    )
    @POST("token")
    fun registerPushToken(
        @Body request: PushTokenRequest,
    ): Call<Unit>

    @Headers(
        "x-datadog-sampling-priority: 1",
        "Content-Type: application/json",
    )
    @POST("mtctrl")
    fun sendDirectOpenStatus(
        @Body request: DirectOpenRequest,
    ): Call<Unit>

    @Headers("Content-Type: application/json")
    @POST("opt-in-subscriptions")
    fun optInSubscription(
        @Body request: OptInSubscriptionRequest,
    ): Call<Unit>

    @Headers("Content-Type: application/json")
    @POST("opt-out-subscriptions")
    fun optOutSubscription(
        @Body request: OptOutSubscriptionRequest,
    ): Call<Unit>
}
