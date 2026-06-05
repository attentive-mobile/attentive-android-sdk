package com.attentive.androidsdk

import android.app.Application
import android.content.Context
import androidx.annotation.ColorRes
import com.attentive.androidsdk.internal.events.InfoEvent
import com.attentive.androidsdk.internal.network.ApiVersion
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.androidsdk.internal.util.StandardTree
import com.attentive.androidsdk.internal.util.VerboseTree
import okhttp3.OkHttpClient
import timber.log.Timber

/**
 * SDK-wide configuration created via [Builder] and passed to [AttentiveSdk.initialize].
 *
 * Holds the Attentive domain, runtime mode, notification styling, log level, and the
 * current [UserIdentifiers]. Construct with [Builder] and hand to [AttentiveSdk.initialize]
 * during `Application.onCreate()`. Construction fires an `InfoEvent` to the Attentive
 * backend as a ping.
 */
class AttentiveConfig private constructor(builder: Builder) : AttentiveConfigInterface {
    override val mode = builder._mode
    override var domain: String = builder._domain
    override val applicationContext = builder._context
    override var notificationIconId: Int = builder._notificationIconId
    override var notificationIconBackgroundColorResource: Int =
        builder._notificationIconBackgroundColorResource
    override var logLevel: AttentiveLogLevel? = null

    private val visitorService =
        ClassFactory.buildVisitorService(ClassFactory.buildPersistentStorage(builder._context))

    override var userIdentifiers =
        UserIdentifiers.Builder().withVisitorId(visitorService.visitorId).build()

    internal val attentiveApi: AttentiveApi
    private val skipFatigueOnCreatives: Boolean = builder.skipFatigueOnCreatives
    private val settingsService: SettingsService =
        ClassFactory.buildSettingsService(ClassFactory.buildPersistentStorage(builder._context))

    var apiVersion = ApiVersion.OLD

    init {
        Timber.d("Initializing AttentiveConfig with configuration: %s", builder)
        logLevel = builder.logLevel
        apiVersion = builder.apiVersion
        configureLogging(logLevel, settingsService, builder._context)

        val okHttpClient =
            builder.okHttpClient ?: ClassFactory.buildOkHttpClient(
                logLevel,
                ClassFactory.buildUserAgentInterceptor(builder._context),
                builder._context,
            )
        attentiveApi = ClassFactory.buildAttentiveApi(okHttpClient, domain)
        sendInfoEvent()
    }

    override fun skipFatigueOnCreatives(): Boolean {
        return skipFatigueOnCreatives
    }

    override fun identify(clientUserId: String) {
        Timber.i("identify called with clientUserId: %s", clientUserId)
        ParameterValidation.verifyNotEmpty(clientUserId, "clientUserId")
        identify(UserIdentifiers.Builder().withClientUserId(clientUserId).build())
    }

    /**
     * Merges the given identifiers into the current visitor and notifies the backend.
     * Does not change the visitor ID. Identifiers accumulate across calls. Fires
     * `POST /e?t=idn` to the Attentive backend.
     */
    override fun identify(userIdentifiers: UserIdentifiers) {
        this.userIdentifiers = UserIdentifiers.merge(this.userIdentifiers, userIdentifiers)
        Timber.i("identify called with userIdentifiers: %s", this.userIdentifiers)
        sendUserIdentifiersCollectedEvent()
    }

    internal fun resetIdentifiers() {
        Timber.i("Resetting user identifiers with new visitor ID")
        val newVisitorId = visitorService.createNewVisitorId()
        userIdentifiers = UserIdentifiers.Builder().withVisitorId(newVisitorId).build()
    }

    /**
     * Clears local user identifiers and generates a new visitor ID. Does **not** notify the
     * backend — the push token remains associated with the prior user server-side. For a
     * full logout that detaches the push token on the backend, use [AttentiveSdk.clearUser]
     * instead.
     */
    @Deprecated(
        message = "Use AttentiveSdk.clearUser() instead. It properly detaches the push token from the logged-out user.",
        replaceWith = ReplaceWith("AttentiveSdk.clearUser()")
    )
    override fun clearUser() {
        Timber.i("clearUser called")
        resetIdentifiers()
    }

    /**
     * Changes the Attentive domain at runtime. Fires a fresh `InfoEvent` if the domain
     * changed.
     *
     * @throws IllegalArgumentException if [domain] is empty or contains `attn.tv`, `:`, or `/`.
     */
    override fun changeDomain(domain: String) {
        domain.verifyValidAttentiveDomain()
        if (domain != this.domain) {
            this.domain = domain
            sendInfoEvent()
        }
    }

    /**
     * Changes the event-API version at runtime. Debug/testing use only.
     */
    fun changeApiVersion(apiVersion: ApiVersion) {
        Timber.d("Changing API version from ${this@AttentiveConfig.apiVersion} to $apiVersion")
        this@AttentiveConfig.apiVersion = apiVersion
    }

    private fun sendUserIdentifiersCollectedEvent() {
        attentiveApi.sendUserIdentifiersCollectedEvent(
            domain,
            userIdentifiers,
            object : AttentiveApiCallback {
                override fun onFailure(message: String?) {
                    Timber.e("Could not send the user identifiers. Error: %s", message)
                }

                override fun onSuccess() {
                    Timber.i("Successfully sent the user identifiers")
                }
            },
        )
    }

    private fun sendInfoEvent() {
        attentiveApi.sendEvent(InfoEvent(), userIdentifiers, domain)
    }

    companion object {
        private fun configureLogging(
            logLevel: AttentiveLogLevel?,
            settingsService: SettingsService,
            context: Context,
        ) {
            val settingsLogLevel = settingsService.logLevel
            if (settingsLogLevel != null) {
                setLogLevel(settingsLogLevel)
                return
            }
            if (logLevel != null) {
                setLogLevel(logLevel)
                return
            }
            if (AppInfo.isDebuggable(context)) {
                setLogLevel(AttentiveLogLevel.VERBOSE)
            }
        }

        private fun setLogLevel(logLevel: AttentiveLogLevel) {
            when (logLevel) {
                AttentiveLogLevel.VERBOSE -> Timber.plant(VerboseTree())
                AttentiveLogLevel.STANDARD -> Timber.plant(StandardTree())
            }
        }
    }

    /**
     * Builder for [AttentiveConfig]. Required: [applicationContext], [mode], [domain].
     *
     * Example:
     * ```
     * val config = AttentiveConfig.Builder()
     *     .applicationContext(application)
     *     .mode(AttentiveConfig.Mode.PRODUCTION)
     *     .domain("mybrand")
     *     .build()
     * AttentiveSdk.initialize(config)
     * ```
     */
    class Builder {
        internal lateinit var _context: Application
        internal lateinit var _mode: Mode
        internal lateinit var _domain: String
        internal var _notificationIconId: Int = 0

        @ColorRes
        internal var _notificationIconBackgroundColorResource: Int = 0
        internal var okHttpClient: OkHttpClient? = null
        internal var skipFatigueOnCreatives: Boolean = false
        internal var logLevel: AttentiveLogLevel = AttentiveLogLevel.STANDARD

        internal var apiVersion: ApiVersion = ApiVersion.OLD

        fun applicationContext(context: Application) =
            apply {
                ParameterValidation.verifyNotNull(context, "context")
                _context = context
            }

        @Deprecated("Use applicationContext() instead. This function will be removed in a future release.")
        fun context(context: Application) =
            apply {
                _context = context
            }

        fun mode(mode: Mode) =
            apply {
                _mode = mode
            }

        /**
         * @throws IllegalArgumentException if [domain] is empty or contains `attn.tv`, `:`, or `/`.
         */
        fun domain(domain: String) =
            apply {
                domain.verifyValidAttentiveDomain()
                _domain = domain
            }

        fun notificationIconId(notificationIconId: Int) =
            apply {
                _notificationIconId = notificationIconId
            }

        fun notificationIconBackgroundColor(
            @ColorRes colorResourceId: Int,
        ) = apply {
            _notificationIconBackgroundColorResource = colorResourceId
        }

        private val allowApiVersionOverride = false

        /**
         * Currently a no-op for integrators — gated behind a private flag.
         */
        fun apiVersion(apiVersion: ApiVersion) =
            apply {
                if (allowApiVersionOverride) {
                    this.apiVersion = apiVersion
                }
            }

        fun okHttpClient(okHttpClient: OkHttpClient) =
            apply {
                this.okHttpClient = okHttpClient
            }

        fun skipFatigueOnCreatives(skipFatigueOnCreatives: Boolean) =
            apply {
                this.skipFatigueOnCreatives = skipFatigueOnCreatives
            }

        fun logLevel(logLevel: AttentiveLogLevel) =
            apply {
                this.logLevel = logLevel
            }

        /**
         * @throws IllegalStateException if [applicationContext], [mode], or [domain] was not set.
         */
        fun build(): AttentiveConfig {
            if (this::_context.isInitialized.not()) {
                throw IllegalStateException("A valid context must be provided.")
            }
            if (this::_mode.isInitialized.not()) {
                throw IllegalStateException("A valid mode must be provided.")
            }
            if (this::_domain.isInitialized.not()) {
                throw IllegalStateException("A valid domain must be provided.")
            }
            return AttentiveConfig(this)
        }

        override fun toString(): String {
            return "Builder(context=$_context, mode=$_mode, domain=$_domain, okHttpClient=$okHttpClient, " +
                "skipFatigueOnCreatives=$skipFatigueOnCreatives, logLevel=$logLevel)"
        }
    }

    /**
     * SDK runtime mode.
     */
    enum class Mode {
        /** Debug mode — verbose logging by default, intended for development. */
        DEBUG,

        /** Production mode — standard logging, intended for release builds. */
        PRODUCTION,
    }
}

private fun String.verifyValidAttentiveDomain() {
    if (this.isEmpty()) {
        throw IllegalArgumentException("Domain cannot be empty")
    }
    if (this.contains("attn.tv") || this.contains(":") || this.contains("/")) {
        throw IllegalArgumentException("The provided domain $this is not recognized. Please verify that the domain matches your Attentive settings.")
    }
}
