package com.attentive.androidsdk.internal.network

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class OptInSubscriptionRequest(
    @SerializedName("c")
    val company: String,
    @SerializedName("v")
    val version: String,
    @SerializedName("u")
    val visitorId: String,
    @SerializedName("evs")
    val externalVendorIds: JsonElement,
    @SerializedName("tp")
    val tokenProvider: String = "fcm",
    @SerializedName("pt")
    val pushToken: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("type")
    val type: String = "MARKETING",
)

data class OptOutSubscriptionRequest(
    @SerializedName("c")
    val company: String,
    @SerializedName("v")
    val version: String,
    @SerializedName("u")
    val visitorId: String,
    @SerializedName("evs")
    val externalVendorIds: JsonElement,
    @SerializedName("tp")
    val tokenProvider: String = "fcm",
    @SerializedName("pt")
    val pushToken: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("phone")
    val phone: String?,
)
