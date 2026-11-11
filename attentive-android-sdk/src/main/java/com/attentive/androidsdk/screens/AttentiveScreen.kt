package com.attentive.androidsdk.screens

/**
 * A screen the user navigated to.
 *
 * @property name snake_case screen name, e.g. `product_detail`.
 * @property route the navigation route it was resolved from, without arguments, or null when it
 * was resolved from a destination label.
 */
data class AttentiveScreen(
    val name: String,
    val route: String?,
)
