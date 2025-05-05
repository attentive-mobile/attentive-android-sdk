package com.attentive.androidsdk

import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.push.TokenFetchResult
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

class AttentiveEventTracker private constructor() {
    var config: AttentiveConfig? = null

    fun initialize(config: AttentiveConfig) {
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
        verifyInitialized()

        config?.let {
            it.attentiveApi.sendEvent(event, it.userIdentifiers, it.domain)
        }
    }

    fun registerPushToken(){
        verifyInitialized()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if(task.isSuccessful) {
                config?.let {
                    it.attentiveApi.registerPushToken(token = task.result, it.userIdentifiers, it.domain)
                }
            }
        }
    }

    fun getPushToken(callback: (Result<TokenFetchResult>) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(Result.success(TokenFetchResult(task.result)))
            } else {
                callback(Result.failure(Exception("Failed to fetch token")))
            }
        }
    }

    private fun verifyInitialized() {
        synchronized(AttentiveEventTracker::class.java) {
            if (config == null) {
                Timber.e("AttentiveEventTracker must be initialized with an AttentiveConfig before use.")
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
