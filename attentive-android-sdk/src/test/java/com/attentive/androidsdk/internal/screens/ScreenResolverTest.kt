package com.attentive.androidsdk.internal.screens

import com.attentive.androidsdk.screens.AttentiveScreen
import com.attentive.androidsdk.screens.AttentiveScreenCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenResolverTest {
    private val catalog =
        object : AttentiveScreenCatalog {
            override val names = mapOf("ProductScreenRoute" to "pdp")
            override val ignored = setOf("SplashScreen")
        }

    @Test
    fun resolve_noCatalog_derivesNameFromRoute() {
        assertEquals(
            ScreenResolution.Named(AttentiveScreen("product", "product")),
            ScreenResolver.resolve("product/{id}", null, null),
        )
    }

    @Test
    fun resolve_routeInCatalog_usesCatalogName() {
        assertEquals(
            ScreenResolution.Named(AttentiveScreen("pdp", "ProductScreenRoute")),
            ScreenResolver.resolve("ProductScreenRoute", null, catalog),
        )
    }

    @Test
    fun resolve_routeNotInCatalog_derivesNameFromRoute() {
        assertEquals(
            ScreenResolution.Named(AttentiveScreen("cart", "CartScreen")),
            ScreenResolver.resolve("CartScreen", null, catalog),
        )
    }

    @Test
    fun resolve_ignoredRoute_isIgnored() {
        assertEquals(ScreenResolution.Ignored("SplashScreen"), ScreenResolver.resolve("SplashScreen", null, catalog))
    }

    @Test
    fun resolve_catalogLookupIgnoresArguments() {
        val typeSafeCatalog =
            object : AttentiveScreenCatalog {
                override val names = mapOf("com.example.Route.Product" to "pdp")
                override val ignored = emptySet<String>()
            }

        assertEquals(
            ScreenResolution.Named(AttentiveScreen("pdp", "com.example.Route.Product")),
            ScreenResolver.resolve("com.example.Route.Product/{sku}?ref={ref}", null, typeSafeCatalog),
        )
    }

    @Test
    fun resolve_noRoute_fallsBackToLabel() {
        assertEquals(
            ScreenResolution.Named(AttentiveScreen("order_history", null)),
            ScreenResolver.resolve(null, "Order History", catalog),
        )
    }

    @Test
    fun resolve_noRouteOrLabel_isUnresolved() {
        assertEquals(ScreenResolution.Unresolved, ScreenResolver.resolve(null, null, catalog))
        assertEquals(ScreenResolution.Unresolved, ScreenResolver.resolve("{id}", " ", null))
    }
}
