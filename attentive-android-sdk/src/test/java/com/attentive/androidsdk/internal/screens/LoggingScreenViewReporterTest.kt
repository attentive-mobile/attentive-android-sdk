package com.attentive.androidsdk.internal.screens

import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.FakeAttentiveIdentityProvider
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.androidsdk.screens.AttentiveScreen
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class LoggingScreenViewReporterTest {
    private val logged = mutableListOf<String>()
    private var config: AttentiveConfig? = config(UserIdentifiers.Builder().withVisitorId("someVisitorId").build())
    private val reporter = LoggingScreenViewReporter({ config }) { logged += it }

    @Test
    fun report_notInitialized_logsScreenName() {
        config = null

        reporter.report(AttentiveScreen("cart", "CartScreen"))

        assertEquals(listOf("Would send screen view cart; SDK is not initialized"), logged)
    }

    @Test
    fun report_noVisitorId_logsScreenName() {
        config = config(UserIdentifiers.Builder().build())

        reporter.report(AttentiveScreen("cart", "CartScreen"))

        assertEquals(listOf("Would send screen view cart; no visitor ID"), logged)
    }

    @Test
    fun report_initialized_logsPageViewRequest() {
        reporter.report(AttentiveScreen("product_list", "ProductScreenRoute"))

        val request = loggedRequest(0)
        assertEquals("PageView", request.string("eventType"))
        assertEquals("product_list", request.string("locationHref"))
        assertEquals("", request.string("referrer"))
        assertEquals("someVisitorId", request.string("visitorId"))
        assertEquals("games", request.string("attentiveDomain"))
        assertEquals("mobile", request.string("sourceType"))
    }

    @Test
    fun report_secondScreen_usesPreviousScreenAsReferrer() {
        reporter.report(AttentiveScreen("product_list", "ProductScreenRoute"))
        reporter.report(AttentiveScreen("cart", "CartScreen"))

        val request = loggedRequest(1)
        assertEquals("cart", request.string("locationHref"))
        assertEquals("product_list", request.string("referrer"))
    }

    private fun config(userIdentifiers: UserIdentifiers): AttentiveConfig {
        val config = mock<AttentiveConfig>()
        whenever(config.attentiveApi).thenReturn(AttentiveApi(OkHttpClient(), "games", FakeAttentiveIdentityProvider()))
        whenever(config.userIdentifiers).thenReturn(userIdentifiers)
        whenever(config.domain).thenReturn("games")
        return config
    }

    private fun loggedRequest(index: Int): JsonObject =
        Json.parseToJsonElement(logged[index].removePrefix("Would send screen view: ")).jsonObject

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
}
