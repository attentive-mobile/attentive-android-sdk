package com.attentive.androidsdk.internal.screens

import com.attentive.androidsdk.screens.AttentiveScreen
import com.attentive.androidsdk.screens.AttentiveScreenCatalog

internal sealed class ScreenResolution {
    data class Named(val screen: AttentiveScreen) : ScreenResolution()

    data class Ignored(val route: String) : ScreenResolution()

    object Unresolved : ScreenResolution()
}

internal object ScreenResolver {
    /**
     * Resolves a destination to a screen. A route is looked up in [catalog] first and otherwise
     * derived with [ScreenNameFormatter]. Destinations without a route, such as Fragment
     * destinations declared in a navigation XML graph, fall back to their label.
     */
    fun resolve(
        route: String?,
        label: CharSequence?,
        catalog: AttentiveScreenCatalog?,
    ): ScreenResolution {
        if (route != null) {
            val key = ScreenNameFormatter.routeKey(route)
            if (catalog != null && key in catalog.ignored) {
                return ScreenResolution.Ignored(key)
            }
            val name = catalog?.names?.get(key) ?: ScreenNameFormatter.fromRoute(route)
            if (name != null) {
                return ScreenResolution.Named(AttentiveScreen(name, key))
            }
        }

        val labelName = label?.toString()?.let(ScreenNameFormatter::fromLabel)
        if (labelName != null) {
            return ScreenResolution.Named(AttentiveScreen(labelName, null))
        }
        return ScreenResolution.Unresolved
    }
}
