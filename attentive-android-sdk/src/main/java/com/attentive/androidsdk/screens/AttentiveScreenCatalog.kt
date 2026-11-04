package com.attentive.androidsdk.screens

/**
 * Screen names for a set of routes, usually generated from a type annotated with
 * [AttentiveScreens]. Keys are routes without their arguments, as navigation reports them.
 */
interface AttentiveScreenCatalog {
    /** Screen name for each route. */
    val names: Map<String, String>

    /** Routes that are never tracked. */
    val ignored: Set<String>
}
