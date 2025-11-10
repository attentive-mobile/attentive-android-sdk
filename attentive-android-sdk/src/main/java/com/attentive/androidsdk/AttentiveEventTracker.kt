package com.attentive.androidsdk

import android.content.Context
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.internal.network.ApiVersion
import com.attentive.androidsdk.push.AttentivePush
import com.attentive.androidsdk.push.TokenProvider
import com.attentive.androidsdk.tracking.AppLaunchTracker
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume


/**
 * To use the AttentiveEventTracker you must first initialize it with an AttentiveConfig.
 */
class AttentiveEventTracker private constructor() {
    lateinit var config: AttentiveConfig
    internal lateinit var launchTracker: AppLaunchTracker

    fun initialize(config: AttentiveConfig) {
        Timber.d(
            "Initializing Attentive SDK with attn domain %s and mode %s",
            config.domain,
            config.mode
        )

        synchronized(AttentiveEventTracker::class.java) {
            if (::config.isInitialized) {
                Timber.e("Attempted to re-initialize AttentiveEventTracker - please initialize once per runtime")
            }
            this.config = config



            if (!::launchTracker.isInitialized) {
                Timber.d("Initializing AppLaunchTracker")
                launchTracker = AppLaunchTracker(config.applicationContext)
            } else {
                Timber.d("AppLaunchTracker already initialized")
            }
        }
    }

    /**
     * Records an event. Uses callback-based approach for non-suspend contexts.
     * From Java, this is the version that will be called.
     *
     * Supports the following event types:
     * - PurchaseEvent -> Maps to Purchase EventMetadata
     * - ProductViewEvent -> Maps to ProductView EventMetadata
     * - AddToCartEvent -> Maps to AddToCart EventMetadata
     * - CustomEvent -> Maps to MobileCustomEvent EventMetadata
     *
     * @param event The event to record
     * @param callback Optional callback to handle success or failure
     */
    @JvmOverloads
    fun recordEvent(event: Event, callback: AttentiveApiCallback? = null) {
        verifyInitialized()

        config?.let {
            if(it.apiVersion == ApiVersion.OLD) {
                it.attentiveApi.sendEvent(event, it.userIdentifiers, it.domain, callback)
            } else {
                it.attentiveApi.recordEventCall(
                    event,
                    it.userIdentifiers,
                    it.domain,
                    callback ?: object : AttentiveApiCallback {
                        override fun onSuccess() {
                            Timber.d("Event recorded successfully")
                        }

                        override fun onFailure(message: String?) {
                            Timber.e("Failed to record event: $message")
                        }
                    }
                )
            }
        }
    }

    /**
     * Records an event (suspend version).
     * This is a suspend function that should be called from a coroutine context.
     * From Kotlin suspend contexts, this version will be automatically selected.
     *
     * Supports both OLD and NEW API versions:
     * - OLD: Uses callback-based sendEvent wrapped in suspendCancellableCoroutine
     * - NEW: Uses native suspend recordEvent
     *
     * Currently supports the following event types:
     * - PurchaseEvent -> Maps to Purchase EventMetadata
     * - ProductViewEvent -> Maps to ProductView EventMetadata
     * - AddToCartEvent -> Maps to AddToCart EventMetadata
     * - CustomEvent -> Maps to MobileCustomEvent EventMetadata
     *
     * @param event The event to record
     */
    suspend fun recordEvent(event: Event) {
        verifyInitialized()

        if(config.apiVersion == ApiVersion.OLD) {
            // Wrap the callback-based old API in a suspend function
            suspendCancellableCoroutine { continuation ->
                config.attentiveApi.sendEvent(
                    event,
                    config.userIdentifiers,
                    config.domain,
                    object : AttentiveApiCallback {
                        override fun onSuccess() {
                            Timber.d("Event recorded successfully (OLD API)")
                            continuation.resume(Unit)
                        }

                        override fun onFailure(message: String?) {
                            Timber.e("Failed to record event (OLD API): $message")
                            continuation.resume(Unit) // Resume anyway, don't throw
                        }
                    }
                )
            }
        } else {
            config.attentiveApi.recordEvent(event, config.userIdentifiers, config.domain)
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
            config.let {
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
                } else {
                    Timber.e("Failed to fetch push token: ${it.exceptionOrNull()?.message}")
                }
            }
        }
    }

    internal suspend fun optIn(email: String = "", phoneNumber: String = "" ) {
        verifyInitialized()
        if (phoneNumber.isEmpty() && email.isEmpty()) {
            Timber.e("At least one of phone number or email must be provided to opt in.")
            return
        }
        TokenProvider.getInstance().getToken(config.applicationContext).let {
            if (it.isSuccess) {
                config.attentiveApi.sendOptInSubscriptionStatus(
                    phoneNumber,
                    email,
                    it.getOrNull()?.token
                )
            }
        }
    }

    internal suspend fun optOut( email: String = "", phoneNumber: String = "",) {
        verifyInitialized()
        if (phoneNumber.isEmpty() && email.isEmpty()) {
            Timber.e("At least one of phone number or email must be provided to opt out.")
            return
        }
        TokenProvider.getInstance().getToken(config.applicationContext).let {
            if (it.isSuccess) {
                config.attentiveApi.sendOptOutSubscriptionStatus(
                    email,
                    phoneNumber,
                    config.domain,
                    it.getOrNull()?.token
                )
            }
        }
    }


    private fun verifyInitialized() {
        synchronized(AttentiveEventTracker::class.java) {
            if (!::config.isInitialized) {
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

