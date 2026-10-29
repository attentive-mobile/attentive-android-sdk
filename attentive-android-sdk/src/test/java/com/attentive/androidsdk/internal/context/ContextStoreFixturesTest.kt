package com.attentive.androidsdk.internal.context

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Runs `context-store-fixtures.json`: each case is a sequence of steps and the store state
 * expected after them. Only the keys present in `expect` are compared. The same fixtures are
 * meant to run against every SDK, so platforms agree on what a session and a screen are.
 */
class ContextStoreFixturesTest {
    private val json = Json { encodeDefaults = true; explicitNulls = true }

    @Test
    fun fixtures() {
        val fixtures = loadFixtures()
        val failures =
            fixtures.mapNotNull { fixture ->
                val name = fixture.getValue("name").jsonPrimitive.content
                val actual = run(fixture.getValue("steps").jsonArray)
                val mismatches = mismatches("", fixture.getValue("expect"), actual)
                if (mismatches.isEmpty()) null else "$name:\n  " + mismatches.joinToString("\n  ")
            }
        assertEquals("Failing fixtures:\n" + failures.joinToString("\n"), emptyList<String>(), failures)
    }

    private fun run(steps: JsonArray): JsonObject {
        val storage = InMemoryStorage()
        var now = 0L
        var store = PersistentContextStore(storage.mock, "com.example.shop") { now }
        val parser = OpenUrlParser()
        for (step in steps.map { it.jsonObject }) {
            now = step.getValue("atMs").jsonPrimitive.long
            when (val op = step.getValue("op").jsonPrimitive.content) {
                "foreground" -> store.onForeground(now)
                "background" -> store.onBackground(now)
                "screen" -> store.onScreenView(step.getValue("name").jsonPrimitive.content, now)
                "productView" -> store.onProductView(products(step, now))
                "addToCart" -> store.onAddToCart(products(step, now))
                "purchase" -> store.onPurchase()
                "openUrl" -> {
                    val opened = requireNotNull(parser.parse(step.getValue("url").jsonPrimitive.content))
                    store.onLaunch(opened.source, opened.utms, opened.landingUrl)
                }
                "clear" -> store.clear()
                "sessionStart" -> store.onSessionStart()
                "restart" -> store = PersistentContextStore(storage.mock, "com.example.shop") { now }
                "read" -> Unit
                else -> error("Unknown op $op")
            }
        }
        return toFixtureJson(store.snapshot())
    }

    private fun products(step: JsonObject, atMs: Long): List<ContextProduct> =
        step.getValue("productIds").jsonArray.map {
            ContextProduct(it.jsonPrimitive.content, null, null, null, null, null, atMs)
        }

    /** The snapshot in fixture form: products as IDs and UTMs as plain values. */
    private fun toFixtureJson(context: AppContext): JsonObject =
        buildJsonObject {
            put("screen", json.encodeToJsonElement(ScreenContext.serializer(), context.screen))
            put(
                "products",
                buildJsonObject {
                    put("recentlyViewedIds", JsonArray(context.products.recentlyViewed.map { JsonPrimitive(it.productId) }))
                    put(
                        "recentlyAddedToCartIds",
                        JsonArray(context.products.recentlyAddedToCart.map { JsonPrimitive(it.productId) }),
                    )
                },
            )
            val launch = json.encodeToJsonElement(LaunchContext.serializer(), context.launch).jsonObject
            put(
                "launch",
                JsonObject(
                    launch +
                        (
                            "utmParams" to
                                JsonObject(
                                    launch.getValue("utmParams").jsonObject.mapValues {
                                        it.value.jsonObject.getValue("value")
                                    },
                                )
                        ),
                ),
            )
        }

    private fun mismatches(path: String, expected: JsonElement, actual: JsonElement?): List<String> =
        if (expected is JsonObject && actual is JsonObject && path.endsWith("utmParams").not()) {
            expected.flatMap { (key, value) -> mismatches("$path.$key", value, actual[key]) }
        } else if (expected != actual) {
            listOf("$path: expected $expected, was $actual")
        } else {
            emptyList()
        }

    private fun loadFixtures(): List<JsonObject> {
        val text =
            requireNotNull(javaClass.classLoader?.getResource("context-store-fixtures.json")) {
                "context-store-fixtures.json is missing from test resources"
            }.readText()
        return Json.parseToJsonElement(text).jsonArray.map { it.jsonObject }
    }
}
