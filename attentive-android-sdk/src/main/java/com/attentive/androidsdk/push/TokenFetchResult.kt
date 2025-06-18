package com.attentive.androidsdk.push

data class TokenFetchResult(
    val token: String,
    val permissionGranted: Boolean
)
