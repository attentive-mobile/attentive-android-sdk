package com.attentive.androidsdk.internal.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RetrofitCdnApiService {
    @GET("{domain}/dtag.js")
    fun getDtag(
        @Path("domain") domain: String,
    ): Call<ResponseBody>
}
