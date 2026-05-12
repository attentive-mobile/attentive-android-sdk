package com.attentive.androidsdk

/**
 * Generic success/failure callback for internal Attentive API requests. Used by internal
 * callback-based network code.
 */
interface AttentiveApiCallback {
    fun onFailure(message: String?)

    fun onSuccess()
}
