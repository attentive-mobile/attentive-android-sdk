package com.attentive.androidsdk

import com.attentive.androidsdk.events.Event
import timber.log.Timber

class AttentiveEventTracker private constructor() {
    private var config: AttentiveConfig? = null

    fun initialize(config: AttentiveConfig) {
        ParameterValidation.verifyNotNull(config, "config")
        Timber.i(
            "Initializing Attentive SDK with attn domain %s and mode %s",
            config.domain,
            config.mode
        )

        synchronized(AttentiveEventTracker::class.java) {
            if (this.config != null) {
                Timber.w("Attempted to re-initialize AttentiveEventTracker - please initialize once per runtime")
            }
            this.config = config
        }
    }

    fun recordEvent(event: Event) {
        ParameterValidation.verifyNotNull(event, "event")
        verifyInitialized()

        config?.let {
            it.attentiveApi.sendEvent(event, it.userIdentifiers, it.domain)
        }
    }

    private fun verifyInitialized() {
        synchronized(AttentiveEventTracker::class.java) {
            if (INSTANCE == null) {
                Timber.e("AttentiveEventTracker must be initialized before use.")
            }
        }
    }

    companion object {
        private var INSTANCE: AttentiveEventTracker? = null

        @JvmStatic
        val instance: AttentiveEventTracker
            get() {
                synchronized(AttentiveEventTracker::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = AttentiveEventTracker()
                    }
                    return INSTANCE!!
                }
            }
    }
}
