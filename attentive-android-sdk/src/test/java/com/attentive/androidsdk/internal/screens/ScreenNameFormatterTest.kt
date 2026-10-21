package com.attentive.androidsdk.internal.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenNameFormatterTest {
    @Test
    fun fromRoute_enumStyleRoute_stripsSuffixesAndSnakeCases() {
        assertEquals("welcome", ScreenNameFormatter.fromRoute("WelcomeScreenRoute"))
        assertEquals("product", ScreenNameFormatter.fromRoute("ProductScreenRoute"))
        assertEquals("thank_you", ScreenNameFormatter.fromRoute("ThankYouScreen"))
        assertEquals("legacy_inbox", ScreenNameFormatter.fromRoute("LegacyInboxScreen"))
    }

    @Test
    fun fromRoute_stringRouteWithArguments_dropsArguments() {
        assertEquals("product", ScreenNameFormatter.fromRoute("product/{id}"))
        assertEquals("product", ScreenNameFormatter.fromRoute("product/{id}?ref={ref}"))
        assertEquals("cart", ScreenNameFormatter.fromRoute("cart?coupon={coupon}"))
    }

    @Test
    fun fromRoute_multipleStaticSegments_joinsThem() {
        assertEquals("settings_notifications", ScreenNameFormatter.fromRoute("settings/notifications"))
        assertEquals("orders_detail", ScreenNameFormatter.fromRoute("orders/{orderId}/detail"))
    }

    @Test
    fun fromRoute_typeSafeRoute_dropsPackageAndOuterClasses() {
        assertEquals("product_detail", ScreenNameFormatter.fromRoute("com.example.app.ProductDetailRoute/{sku}"))
        assertEquals("product", ScreenNameFormatter.fromRoute("com.example.app.Route.Product/{sku}?ref={ref}"))
        assertEquals("home", ScreenNameFormatter.fromRoute("com.example.app.Route.Home"))
    }

    @Test
    fun fromRoute_acronymsAndSeparators_snakeCases() {
        assertEquals("pdp", ScreenNameFormatter.fromRoute("PDPScreen"))
        assertEquals("http_error", ScreenNameFormatter.fromRoute("HTTPErrorScreen"))
        assertEquals("order_history", ScreenNameFormatter.fromRoute("order-history"))
        assertEquals("order_history", ScreenNameFormatter.fromRoute("order_history"))
    }

    @Test
    fun fromRoute_nameIsOnlyASuffix_keepsIt() {
        assertEquals("screen", ScreenNameFormatter.fromRoute("Screen"))
        assertEquals("route", ScreenNameFormatter.fromRoute("com.example.Route"))
    }

    @Test
    fun fromRoute_onlyArguments_returnsNull() {
        assertNull(ScreenNameFormatter.fromRoute("{id}"))
        assertNull(ScreenNameFormatter.fromRoute(""))
    }

    @Test
    fun routeKey_dropsArguments() {
        assertEquals("ProductScreenRoute", ScreenNameFormatter.routeKey("ProductScreenRoute"))
        assertEquals("product", ScreenNameFormatter.routeKey("product/{id}?ref={ref}"))
        assertEquals("com.example.Route.Product", ScreenNameFormatter.routeKey("com.example.Route.Product/{sku}"))
    }

    @Test
    fun fromLabel_snakeCasesFreeText() {
        assertEquals("product_detail", ScreenNameFormatter.fromLabel("Product Detail"))
        assertEquals("my_orders", ScreenNameFormatter.fromLabel("  My Orders! "))
        assertNull(ScreenNameFormatter.fromLabel("   "))
    }

    @Test
    fun isValidScreenName_requiresSnakeCase() {
        assertTrue(ScreenNameFormatter.isValidScreenName("product_detail"))
        assertTrue(ScreenNameFormatter.isValidScreenName("pdp2"))
        assertFalse(ScreenNameFormatter.isValidScreenName("ProductDetail"))
        assertFalse(ScreenNameFormatter.isValidScreenName("product-detail"))
        assertFalse(ScreenNameFormatter.isValidScreenName("2fa"))
        assertFalse(ScreenNameFormatter.isValidScreenName(""))
    }
}
