package com.attentive.androidsdk

interface AttentiveApiCallback {
    fun onFailure(message: String?)

    fun onSuccess()
}
