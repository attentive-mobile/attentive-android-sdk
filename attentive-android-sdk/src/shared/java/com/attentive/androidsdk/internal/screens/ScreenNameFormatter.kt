package com.attentive.androidsdk.internal.screens

/**
 * Derives screen names from navigation routes. Compiled into both the SDK and
 * `attentive-screens-processor`, so names generated at build time match names derived at runtime.
 */
internal object ScreenNameFormatter {
    /** Longest screen name kept. Longer names are cut to this length. */
    const val MAX_LENGTH = 64

    private val suffixes = listOf("Route", "Screen", "Destination")

    private val separators = Regex("[\\s\\-.]+")
    private val lowerToUpper = Regex("([a-z0-9])([A-Z])")
    private val acronymToWord = Regex("([A-Z]+)([A-Z][a-z])")
    private val repeatedUnderscores = Regex("_+")

    /**
     * The part of a route that identifies its destination, without arguments.
     * `product/{id}?ref={ref}` becomes `product` and `com.app.Route.Product/{sku}` becomes
     * `com.app.Route.Product`.
     */
    fun routeKey(route: String): String = route.substringBefore('?').substringBefore('/')

    /**
     * Turns a route into a snake_case screen name, or returns null when nothing usable is left.
     * Argument segments are dropped, a type-safe route's package is dropped, and the suffixes
     * `Route`, `Screen` and `Destination` are stripped: `com.app.ProductScreenRoute/{id}` becomes
     * `product` and `settings/notifications` becomes `settings_notifications`.
     */
    fun fromRoute(route: String): String? {
        val segments =
            route.substringBefore('?')
                .split('/')
                .filter { it.isNotBlank() && !it.startsWith('{') }
        if (segments.isEmpty()) return null

        return segments
            .mapIndexed { index, segment ->
                val word = if (index == 0) segment.substringAfterLast('.') else segment
                toSnakeCase(stripSuffixes(word))
            }
            .filter { it.isNotEmpty() }
            .joinToString("_")
            .truncate()
            .ifEmpty { null }
    }

    /** Turns free text such as a destination label into a snake_case screen name. */
    fun fromLabel(label: String): String? = toSnakeCase(label).truncate().ifEmpty { null }

    fun isValidScreenName(name: String): Boolean = name.matches(Regex("[a-z][a-z0-9_]*"))

    private fun String.truncate(): String = take(MAX_LENGTH).trimEnd('_')

    private fun stripSuffixes(word: String): String {
        var result = word
        var stripped = true
        while (stripped) {
            stripped = false
            for (suffix in suffixes) {
                if (result.length > suffix.length && result.endsWith(suffix)) {
                    result = result.removeSuffix(suffix)
                    stripped = true
                }
            }
        }
        return result
    }

    private fun toSnakeCase(text: String): String =
        text.trim()
            .replace(separators, "_")
            .replace(acronymToWord, "$1_$2")
            .replace(lowerToUpper, "$1_$2")
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(repeatedUnderscores, "_")
            .trim('_')
}
