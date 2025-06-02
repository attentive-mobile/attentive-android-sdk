package com.attentive.androidsdk

import android.content.Context
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenFetchResult
import com.attentive.androidsdk.push.TokenProvider
import com.attentive.androidsdk.tracking.AppLaunchTracker
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

class AttentiveEventTracker private constructor() {
    var config: AttentiveConfig? = null
    internal lateinit var launchTracker: AppLaunchTracker

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

        launchTracker = AppLaunchTracker(config.applicationContext)
    }

    fun recordEvent(event: Event) {
        verifyInitialized()

        config?.let {
            it.attentiveApi.sendEvent(event, it.userIdentifiers, it.domain)
        }
    }

    internal suspend fun registerPushToken(context: Context) {
        Timber.d("registerPushToken")
        verifyInitialized()
        var token = ""

        TokenProvider.getInstance().getToken(context).run {
            if (isSuccess) {
                token = getOrNull()?.token ?: ""
                Timber.d("Push token fetched successfully: $token")
            } else {
                Timber.e("Failed to fetch push token: ${exceptionOrNull()?.message}")
            }
        }

        if (token.isNotEmpty()) {
            Timber.d("A push token exists, will register with a non empty token")
            config?.let {
                it.attentiveApi.registerPushToken(
                    token = token,
                    permissionGranted = AttentivePush.getInstance()
                        .checkPushPermission(context),
                    it.userIdentifiers,
                    it.domain
                )
            }
        } else {
            Timber.d("No push token exists, will not register")
        }
    }

    /***
     * If LaunchType is DIRECT_OPEN then we will send both the direct open and app launch events.
     */
    internal suspend fun sendAppLaunchEvent(
        launchType: AttentiveApi.LaunchType,
        callbackMap: Map<String, String> = emptyMap()
    ) {
        verifyInitialized()
        config?.let { config ->
            TokenProvider.getInstance().getToken(config.applicationContext).run {
                if (isSuccess) {
                    var token = getOrNull()?.token ?: ""
                    if (token.isEmpty()) {
                        Timber.e("TokenFetchResult is null")
                    }
                }
                TokenProvider.getInstance().getToken(config.applicationContext).let {
                    if (it.isSuccess) {
                        var token = it.getOrNull()?.token
                        if (token == null) {
                            Timber.e("TokenFetchResult is null")
                            token = ""
                        }
                        val permissionGranted = it.getOrNull()?.permissionGranted!!
                        config.attentiveApi.sendDirectOpenStatus(
                            launchType,
                            token,
                            callbackMap,
                            permissionGranted,
                            config.userIdentifiers,
                            config.domain
                        )
                    }
                }
            }
        }
    }

    /***
     * Fetches the push token from Firebase and optionally shows a permission if permission is not granted.
     * @param requestPermission A boolean indicating whether to request permission if not granted.
     *                          - `true`: Requests permission if not already granted.
     *                          - `false`: Skips permission request and directly fetches the token.
     *                          */
    suspend fun getPushToken(requestPermission: Boolean): Result<TokenFetchResult> {
        config?.let {
            return AttentivePush.getInstance()
                .fetchPushToken(it.applicationContext, requestPermission)
        }

        throw IllegalStateException("AttentiveEventTracker must be initialized with an AttentiveConfig before use.")
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

