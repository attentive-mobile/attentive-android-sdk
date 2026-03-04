package com.attentive.androidsdk.internal.network

import com.google.gson.annotations.SerializedName

data class UserUpdateRequest(
    @SerializedName("c")
    val company: String,
    @SerializedName("u")
    val userId: String,
    @SerializedName("pt")
    val pushToken: String,
    @SerializedName("tp")
    val tokenProvider: String = "fcm",
    @SerializedName("v")
    val sdkVersion: String,
    @SerializedName("m")
    val metadata: ContactInfo,
)

data class ContactInfo(
    var phone: String = "",
    var email: String = "",
)
