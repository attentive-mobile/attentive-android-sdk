package com.attentive.androidsdk.internal.network

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class DirectOpenRequest(
    @SerializedName("events")
    val events: List<LaunchEvent>,
    @SerializedName("device")
    val device: DeviceInfo,
)

data class LaunchEvent(
    @SerializedName("ist")
    val type: String,
    @SerializedName("data")
    val data: Map<String, String>,
)

data class DeviceInfo(
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
    @SerializedName("pd")
    val deepLink: String?,
    @SerializedName("tp")
    val tokenProvider: String = "fcm",
)
