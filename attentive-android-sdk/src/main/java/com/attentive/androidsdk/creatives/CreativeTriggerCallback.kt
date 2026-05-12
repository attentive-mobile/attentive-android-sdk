package com.attentive.androidsdk.creatives

/**
 * Callback for [Creative.trigger] lifecycle events. Implement any subset of methods you
 * care about — all defaults are no-ops.
 */
interface CreativeTriggerCallback {
    /**
     * Invoked when [Creative.trigger] was called but the creative did not open successfully.
     * Causes: no creative is configured for the app, the creative was fatigued, the load
     * timed out (5 seconds), or an unknown exception occurred.
     */
    fun onCreativeNotOpened() {}

    /** Invoked when the creative has opened and is visible to the user. */
    fun onOpen() {}

    /** Invoked when the creative did not close successfully (e.g. WebView was null at close time). */
    fun onCreativeNotClosed() {}

    /** Invoked when the creative has closed successfully. */
    fun onClose() {}
}
