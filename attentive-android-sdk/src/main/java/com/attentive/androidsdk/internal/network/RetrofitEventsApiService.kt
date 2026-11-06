package com.attentive.androidsdk.internal.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface RetrofitEventsApiService {
    @POST("e")
    fun sendEvent(
        @Query("v") version: String,
        @Query("lt") lt: String = "0",
        @Query("tag") tag: String = "modern",
        @Query("evs") externalVendorIds: String,
        @Query("c") domain: String,
        @Query("t") eventType: String,
        @Query("u") visitorId: String?,
        @Query("m") metadata: String,
        @QueryMap extraParameters: Map<String, String> = emptyMap(),
    ): Call<Unit>

    @FormUrlEncoded
    @POST("mobile")
    fun sendMobileEvent(
        @Field("d") eventData: String,
    ): Call<Unit>

    @FormUrlEncoded
    @POST("mobile")
    suspend fun sendMobileEventSuspend(
        @Field("d") eventData: String,
    ): Response<Unit>
}
