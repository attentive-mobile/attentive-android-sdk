package com.attentive.androidsdk.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import com.attentive.androidsdk.internal.screens.ScreenNavigationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Names screens automatically from Jetpack Navigation.
 *
 * Screen names come from the route of each destination (`ProductDetailScreen` becomes
 * `product_detail`), or from an [AttentiveScreenCatalog] when one is passed. Routes are strings
 * that R8 does not rename, so names are the same in minified release builds.
 *
 * Navigation is not a dependency of the SDK. These APIs require the host app to include
 * `androidx.navigation:navigation-runtime`, which `navigation-compose` and `navigation-fragment`
 * already bring in.
 */
object AttentiveScreenTracking {
    private val _currentScreen = MutableStateFlow<AttentiveScreen?>(null)

    /** The most recent screen reported by any tracked [NavController]. */
    @JvmStatic
    val currentScreen: StateFlow<AttentiveScreen?> = _currentScreen.asStateFlow()

    /**
     * Starts tracking screens for [navController]. Close the returned handle to stop.
     *
     * In Compose, prefer [TrackAttentiveScreens], which stops tracking when it leaves composition.
     */
    @JvmStatic
    @JvmOverloads
    fun trackNavigation(
        navController: NavController,
        catalog: AttentiveScreenCatalog? = null,
    ): AutoCloseable {
        val listener = ScreenNavigationListener(catalog) { _currentScreen.value = it }
        navController.addOnDestinationChangedListener(listener)
        return AutoCloseable { navController.removeOnDestinationChangedListener(listener) }
    }
}

/** Tracks screens for [navController] while this composable is in composition. */
@Composable
fun TrackAttentiveScreens(
    navController: NavController,
    catalog: AttentiveScreenCatalog? = null,
) {
    DisposableEffect(navController, catalog) {
        val tracking = AttentiveScreenTracking.trackNavigation(navController, catalog)
        onDispose { tracking.close() }
    }
}
