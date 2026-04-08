package com.attentive.androidsdk.internal.network

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class PushTokenRequest(
    @SerializedName("c")
    val company: String,
    @SerializedName("v")
    val version: String,
    @SerializedName("u")
    val visitorId: String,
    @SerializedName("evs")
    val externalVendorIds: JsonElement,
    @SerializedName("m")
    val metadata: ContactInfo,
    @SerializedName("pt")
    val pushToken: String,
    @SerializedName("st")
    val permissionGranted: String,
    @SerializedName("tp")
    val tokenProvider: String = "fcm",
)
