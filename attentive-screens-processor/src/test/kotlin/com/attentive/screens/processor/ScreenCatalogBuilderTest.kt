package com.attentive.screens.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCatalogBuilderTest {
    @Test
    fun build_derivesNamesAndConstants() {
        val catalog =
            success(
                route("WelcomeScreenRoute"),
                route("ThankYouScreen"),
            )

        assertEquals(
            listOf(
                CatalogScreen("WELCOME", "welcome", "Routes.WelcomeScreenRoute.name"),
                CatalogScreen("THANK_YOU", "thank_you", "Routes.ThankYouScreen.name"),
            ),
            catalog.screens,
        )
    }

    @Test
    fun build_override_replacesDerivedName() {
        val catalog = success(route("ProductScreenRoute", nameOverride = "pdp"))

        assertEquals(listOf(CatalogScreen("PDP", "pdp", "Routes.ProductScreenRoute.name")), catalog.screens)
    }

    @Test
    fun build_ignoredRoute_isExcludedFromScreens() {
        val catalog = success(route("CartScreen"), route("SplashScreen", ignored = true))

        assertEquals(listOf("cart"), catalog.screens.map { it.screenName })
        assertEquals(listOf("Routes.SplashScreen.name"), catalog.ignoredKeyExpressions)
    }

    @Test
    fun build_duplicateNames_fails() {
        val errors = failure(route("InboxScreen"), route("InboxRoute"))

        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("Routes.InboxScreen, Routes.InboxRoute"))
        assertTrue(errors.single().contains("\"inbox\""))
    }

    @Test
    fun build_overrideResolvesDuplicate() {
        val catalog = success(route("InboxScreen"), route("InboxRoute", nameOverride = "legacy_inbox"))

        assertEquals(listOf("inbox", "legacy_inbox"), catalog.screens.map { it.screenName })
    }

    @Test
    fun build_duplicateWithIgnoredRoute_succeeds() {
        val catalog = success(route("InboxScreen"), route("InboxRoute", ignored = true))

        assertEquals(listOf("inbox"), catalog.screens.map { it.screenName })
    }

    @Test
    fun build_invalidOverride_fails() {
        val errors = failure(route("ProductScreenRoute", nameOverride = "Product Detail"))

        assertTrue(errors.single().contains("must be snake_case"))
    }

    @Test
    fun build_overrideAndIgnore_fails() {
        val errors = failure(route("SplashScreen", nameOverride = "splash", ignored = true))

        assertTrue(errors.single().contains("both @ScreenName and @IgnoreScreen"))
    }

    @Test
    fun build_leadingDigit_prefixesConstant() {
        val catalog = success(route("404Screen"))

        assertEquals("SCREEN_404", catalog.screens.single().constantName)
    }

    private fun route(
        name: String,
        nameOverride: String? = null,
        ignored: Boolean = false,
    ) = RouteDeclaration(
        sourceName = "Routes.$name",
        keyExpression = "Routes.$name.name",
        routeName = name,
        nameOverride = nameOverride,
        ignored = ignored,
    )

    private fun build(vararg routes: RouteDeclaration) =
        ScreenCatalogBuilder.build(
            packageName = "com.example",
            objectName = "RoutesScreens",
            sourceName = "Routes",
            isInternal = false,
            routes = routes.toList(),
        )

    private fun success(vararg routes: RouteDeclaration): ScreenCatalog {
        val result = build(*routes)
        assertTrue("Expected success but got $result", result is CatalogResult.Success)
        return (result as CatalogResult.Success).catalog
    }

    private fun failure(vararg routes: RouteDeclaration): List<String> {
        val result = build(*routes)
        assertTrue("Expected failure but got $result", result is CatalogResult.Failure)
        return (result as CatalogResult.Failure).errors
    }
}
