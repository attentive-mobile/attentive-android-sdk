package com.attentive.androidsdk.internal.screens

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.attentive.androidsdk.screens.AttentiveScreen
import com.attentive.androidsdk.screens.AttentiveScreenCatalog
import timber.log.Timber

/**
 * Resolves each destination change to a screen and reports it once per back stack entry, so
 * re-notifications for the entry already on screen are dropped.
 */
internal class ScreenNavigationListener(
    private val catalog: AttentiveScreenCatalog?,
    private val onScreen: (AttentiveScreen) -> Unit,
) : NavController.OnDestinationChangedListener {
    private var lastEntryId: String? = null

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?,
    ) {
        val entryId = controller.currentBackStackEntry?.id
        if (entryId != null && entryId == lastEntryId) return
        lastEntryId = entryId

        when (val resolution = ScreenResolver.resolve(destination.route, destination.label, catalog)) {
            is ScreenResolution.Named -> {
                Timber.i("Screen viewed: %s (route: %s)", resolution.screen.name, resolution.screen.route)
                onScreen(resolution.screen)
            }
            is ScreenResolution.Ignored -> Timber.d("Ignoring screen for route %s", resolution.route)
            ScreenResolution.Unresolved -> Timber.w("No screen name for destination %s", destination)
        }
    }
}
