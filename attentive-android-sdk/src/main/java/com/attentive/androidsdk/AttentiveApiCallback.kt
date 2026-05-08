package com.attentive.androidsdk

/**
 * Generic success/failure callback for internal Attentive API requests. Used by internal
 * callback-based network code.
 */
interface AttentiveApiCallback {
    /**
     * Invoked when the request failed.
     *
     * @param message A diagnostic message describing the failure, or `null`.
     */
    fun onFailure(message: String?)

    /**
     * Invoked when the request succeeded.
     */
    fun onSuccess()
}
