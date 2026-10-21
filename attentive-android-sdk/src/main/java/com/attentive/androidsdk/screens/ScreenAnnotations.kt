package com.attentive.androidsdk.screens

/**
 * Generates an [AttentiveScreenCatalog] for an enum or sealed type of navigation routes.
 *
 * Apply `attentive-screens-processor` with KSP, then annotate the type that defines your routes:
 *
 * ```
 * @AttentiveScreens
 * enum class Routes { Home, ProductDetail, @IgnoreScreen Splash }
 * ```
 *
 * The processor writes `RoutesScreens`, which holds a constant per screen and is passed to
 * [AttentiveScreenTracking.trackNavigation].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AttentiveScreens

/** Overrides the screen name derived from a route. Must be snake_case. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class ScreenName(val value: String)

/** Excludes a route from screen tracking. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class IgnoreScreen
