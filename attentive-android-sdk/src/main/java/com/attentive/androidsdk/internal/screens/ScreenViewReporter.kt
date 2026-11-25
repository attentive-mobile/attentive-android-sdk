package com.attentive.androidsdk.internal.screens

import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.internal.network.events.BaseEventRequest
import com.attentive.androidsdk.screens.AttentiveScreen
import kotlinx.serialization.json.Json
import timber.log.Timber

/** Receives each screen the user navigates to. */
internal fun interface ScreenViewReporter {
    fun report(screen: AttentiveScreen)
}

/**
 * Builds the PageView request each screen would be sent as and logs it instead of sending it.
 * Before the SDK is initialized, only the screen name is logged.
 */
internal class LoggingScreenViewReporter(
    private val config: () -> AttentiveConfig? = { AttentiveSdk.configOrNull },
    private val log: (String) -> Unit = { Timber.i(it) },
) : ScreenViewReporter {
    private val json = Json { encodeDefaults = true }
    private var previousScreen: AttentiveScreen? = null

    override fun report(screen: AttentiveScreen) {
        val previous = previousScreen
        previousScreen = screen

        val config = config()
        if (config == null) {
            log("Would send screen view ${screen.name}; SDK is not initialized")
            return
        }

        val request = config.attentiveApi.mapScreenViewEvent(screen, previous, config.userIdentifiers, config.domain)
        if (request == null) {
            log("Would send screen view ${screen.name}; no visitor ID")
            return
        }
        log("Would send screen view: ${json.encodeToString(BaseEventRequest.serializer(), request)}")
    }
}
