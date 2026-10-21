package com.attentive.screens.processor

import com.attentive.androidsdk.internal.screens.ScreenNameFormatter

/**
 * A route declared on a type annotated with `@AttentiveScreens`.
 *
 * @property sourceName how the route is named in error messages, e.g. `Routes.Home`.
 * @property keyExpression Kotlin expression that evaluates to the route key at runtime.
 * @property routeName the name the default screen name is derived from.
 */
internal data class RouteDeclaration(
    val sourceName: String,
    val keyExpression: String,
    val routeName: String,
    val nameOverride: String? = null,
    val ignored: Boolean = false,
)

internal data class CatalogScreen(
    val constantName: String,
    val screenName: String,
    val keyExpression: String,
)

internal data class ScreenCatalog(
    val packageName: String,
    val objectName: String,
    val sourceName: String,
    val isInternal: Boolean,
    val screens: List<CatalogScreen>,
    val ignoredKeyExpressions: List<String>,
)

internal sealed class CatalogResult {
    data class Success(val catalog: ScreenCatalog) : CatalogResult()

    data class Failure(val errors: List<String>) : CatalogResult()
}

internal object ScreenCatalogBuilder {
    fun build(
        packageName: String,
        objectName: String,
        sourceName: String,
        isInternal: Boolean,
        routes: List<RouteDeclaration>,
    ): CatalogResult {
        val errors = mutableListOf<String>()
        val screens = mutableListOf<CatalogScreen>()
        val sourcesByName = mutableMapOf<String, MutableList<String>>()

        for (route in routes.filterNot { it.ignored }) {
            val override = route.nameOverride
            if (override != null && !ScreenNameFormatter.isValidScreenName(override)) {
                errors += "@ScreenName(\"$override\") on ${route.sourceName} must be snake_case, e.g. \"product_detail\""
                continue
            }
            val name = override ?: ScreenNameFormatter.fromRoute(route.routeName)
            if (name == null) {
                errors += "Cannot derive a screen name for ${route.sourceName}; add @ScreenName"
                continue
            }
            sourcesByName.getOrPut(name) { mutableListOf() } += route.sourceName
            screens += CatalogScreen(constantName(name), name, route.keyExpression)
        }

        for ((name, sources) in sourcesByName) {
            if (sources.size > 1) {
                errors += "${sources.joinToString()} all resolve to screen name \"$name\"; use @ScreenName to tell them apart"
            }
        }

        for (route in routes.filter { it.ignored && it.nameOverride != null }) {
            errors += "${route.sourceName} has both @ScreenName and @IgnoreScreen"
        }

        if (errors.isNotEmpty()) return CatalogResult.Failure(errors)

        return CatalogResult.Success(
            ScreenCatalog(
                packageName = packageName,
                objectName = objectName,
                sourceName = sourceName,
                isInternal = isInternal,
                screens = screens,
                ignoredKeyExpressions = routes.filter { it.ignored }.map { it.keyExpression },
            ),
        )
    }

    private fun constantName(screenName: String): String {
        val upper = screenName.uppercase()
        return if (upper.first().isDigit()) "SCREEN_$upper" else upper
    }
}
