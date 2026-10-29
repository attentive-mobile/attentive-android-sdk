package com.attentive.androidsdk.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.internal.context.AttentiveContextStore
import com.attentive.androidsdk.internal.screens.LoggingScreenViewReporter
import com.attentive.androidsdk.internal.screens.ScreenNameFormatter
import com.attentive.androidsdk.internal.screens.ScreenNavigationListener
import com.attentive.androidsdk.internal.screens.ScreenViewReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Names screens automatically from Jetpack Navigation, or reports them by hand with [trackScreen].
 *
 * Screen names come from the route of each destination (`ProductDetailScreen` becomes
 * `product_detail`), or from an [AttentiveScreenCatalog] when one is passed. Routes are strings
 * that R8 does not rename, so names are the same in minified release builds.
 *
 * Navigation is not a dependency of the SDK. The navigation APIs require the host app to include
 * `androidx.navigation:navigation-runtime`, which `navigation-compose` and `navigation-fragment`
 * already bring in.
 */
object AttentiveScreenTracking {
    private val _currentScreen = MutableStateFlow<AttentiveScreen?>(null)

    /** The most recent screen reported by any tracked [NavController]. */
    @JvmStatic
    val currentScreen: StateFlow<AttentiveScreen?> = _currentScreen.asStateFlow()

    internal var reporter: ScreenViewReporter = LoggingScreenViewReporter()

    internal var contextStore: () -> AttentiveContextStore? = { AttentiveSdk.configOrNull?.contextStore }

    internal var clock: () -> Long = System::currentTimeMillis

    /**
     * Reports a screen by name, for screens Jetpack Navigation does not cover, such as Activities
     * and Fragments outside a navigation graph. [name] is converted to snake_case:
     * `Product Detail` and `ProductDetail` both become `product_detail`.
     */
    @JvmStatic
    fun trackScreen(name: String) {
        val screenName = ScreenNameFormatter.fromLabel(name)
        if (screenName == null) {
            Timber.w("Ignoring screen with no usable name: %s", name)
            return
        }
        onScreen(AttentiveScreen(screenName, null))
    }

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
        val listener = ScreenNavigationListener(catalog, ::onScreen)
        navController.addOnDestinationChangedListener(listener)
        return AutoCloseable { navController.removeOnDestinationChangedListener(listener) }
    }

    private fun onScreen(screen: AttentiveScreen) {
        _currentScreen.value = screen
        reporter.report(screen)
        contextStore()?.onScreenView(screen.name, clock())
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
