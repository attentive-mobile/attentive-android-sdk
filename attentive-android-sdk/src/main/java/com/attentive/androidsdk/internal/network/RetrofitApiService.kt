package com.attentive.androidsdk.internal.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitApiService {
    /**
     * Associates or detaches a push token and contact info for a visitor.
     * Used during user switch (updateUser), logout (clearUser), and login (identify)
     * to keep the backend's push token mapping in sync with the current device user.
     * Backend requires pushToken (pt) to be non-blank or it silently discards the request (204).
     */
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
}
