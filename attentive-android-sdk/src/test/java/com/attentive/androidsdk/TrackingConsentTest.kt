package com.attentive.androidsdk

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

/**
 * Wire-contract tests for [TrackingConsent] on the opt-in request.
 *
 * These assert on captured request bytes rather than on the DTO, because the omission rule is a
 * property of the Gson config (no `serializeNulls()`) and isn't visible on the request object.
 *
 * `sendOptIn/OutSubscriptionStatus` read domain and identifiers off the
 * `AttentiveEventTracker` singleton, so these have to populate it. All test classes share one JVM
 * (no `forkEvery`), and seven production branches key off `::config.isInitialized`, so the
 * singleton is restored to its prior value — including uninitialized — after each test rather
 * than left holding this suite's mock.
 */
class TrackingConsentTest {
    private lateinit var bodies: MutableList<String>
    private lateinit var api: AttentiveApi

    /** Whatever the singleton held before this test, `null` if it was never initialized. */
    private var priorConfig: AttentiveConfig? = null

    @Before
    fun setup() {
        bodies = mutableListOf()

        // Read through the field, not the getter: `config` is lateinit, so the getter throws
        // rather than returning null when nothing has initialized it yet.
        priorConfig = CONFIG_FIELD.get(AttentiveEventTracker.instance) as AttentiveConfig?

        val config = Mockito.mock(AttentiveConfig::class.java)
        whenever(config.domain).thenReturn(DOMAIN)
        whenever(config.userIdentifiers)
            .thenReturn(UserIdentifiers.Builder().withVisitorId(VISITOR_ID).build())
        AttentiveEventTracker.instance.config = config

        api = AttentiveApi(recordingClient(), DOMAIN)
    }

    @After
    fun restoreSingletonConfig() {
        // Setting null is only reachable by reflection, and is what makes an uninitialized
        // singleton restorable at all.
        CONFIG_FIELD.set(AttentiveEventTracker.instance, priorConfig)
    }

    // --- toWireValue ------------------------------------------------------------------------

    @Test
    fun toWireValue_acceptedAndDeclined_serializeAsTheirEnumName() {
        assertEquals("ACCEPTED", TrackingConsent.ACCEPTED.toWireValue())
        assertEquals("DECLINED", TrackingConsent.DECLINED.toWireValue())
    }

    @Test
    fun toWireValue_unspecified_isNullSoTheFieldIsOmittedRatherThanSentAsUnspecified() {
        assertEquals(null, TrackingConsent.UNSPECIFIED.toWireValue())
    }

    // --- opt-in ------------------------------------------------------------------------------

    @Test
    fun optIn_accepted_sendsTrackingConsentAccepted() {
        sendOptIn(TrackingConsent.ACCEPTED)
        assertEquals("ACCEPTED", body().get("trackingConsent").asString)
    }

    @Test
    fun optIn_declined_sendsTrackingConsentDeclined() {
        sendOptIn(TrackingConsent.DECLINED)
        assertEquals("DECLINED", body().get("trackingConsent").asString)
    }

    @Test
    fun optIn_unspecified_omitsTheFieldEntirely() {
        sendOptIn(TrackingConsent.UNSPECIFIED)
        val json = body()
        assertFalse(
            "UNSPECIFIED must send no trackingConsent key at all, was: $json",
            json.has("trackingConsent"),
        )
    }

    @Test
    fun optIn_consent_doesNotDisturbTheRestOfTheBody() {
        sendOptIn(TrackingConsent.ACCEPTED)
        val json = body()
        assertEquals(DOMAIN, json.get("c").asString)
        assertEquals(VISITOR_ID, json.get("u").asString)
        assertEquals(EMAIL, json.get("email").asString)
        assertEquals(PHONE, json.get("phone").asString)
        assertEquals("MARKETING", json.get("type").asString)
    }

    // --- opt-out -----------------------------------------------------------------------------

    @Test
    fun optOut_neverSendsTrackingConsent() {
        runBlocking {
            val result = api.sendOptOutSubscriptionStatus(EMAIL, PHONE, DOMAIN, PUSH_TOKEN)
            assertTrue("opt-out failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        }
        val json = body()
        assertFalse("opt-out must not send trackingConsent, was: $json", json.has("trackingConsent"))
    }

    // --- enum shape --------------------------------------------------------------------------

    @Test
    fun enumNames_matchTheBackendContract() {
        // Names are the wire values, so renaming a constant breaks the wire.
        assertEquals(
            listOf("UNSPECIFIED", "ACCEPTED", "DECLINED"),
            TrackingConsent.entries.map { it.name },
        )
    }

    // --- public call shapes ------------------------------------------------------------------

    /**
     * Compile-time guard on the call forms, including the README's example. Never invoked — the
     * point is that they resolve, so a signature change fails here rather than in a host app.
     */
    @Test
    fun publicCallShapes_compile() {
        @Suppress("UNUSED_VARIABLE")
        val shapes: List<suspend () -> Result<Unit>> =
            listOf(
                // Pre-existing forms.
                { AttentiveSdk.optUserIntoMarketingSubscription() },
                { AttentiveSdk.optUserIntoMarketingSubscription(EMAIL) },
                { AttentiveSdk.optUserIntoMarketingSubscription(EMAIL, PHONE) },
                { AttentiveSdk.optUserOutOfMarketingSubscription(EMAIL, PHONE) },
                { AttentiveSdk.optUserIntoMarketingSubscription(email = EMAIL) },
                // The README's example.
                {
                    AttentiveSdk.optUserIntoMarketingSubscription(
                        email = EMAIL,
                        trackingConsent = TrackingConsent.ACCEPTED,
                    )
                },
                // Positional, all three.
                {
                    AttentiveSdk.optUserIntoMarketingSubscription(
                        EMAIL,
                        PHONE,
                        TrackingConsent.ACCEPTED,
                    )
                },
            )
        assertEquals(7, shapes.size)

        // The callback variants take `callback` last, matching the other *WithCallback methods.
        // Because it follows a defaulted parameter, a caller that omits the consent has to name it.
        @Suppress("UNUSED_VARIABLE")
        val callbackShapes: List<() -> Unit> =
            listOf(
                { AttentiveSdk.optUserIntoMarketingSubscriptionWithCallback(EMAIL, PHONE, callback = NOOP) },
                { AttentiveSdk.optUserOutOfMarketingSubscriptionWithCallback(EMAIL, PHONE, callback = NOOP) },
                {
                    AttentiveSdk.optUserIntoMarketingSubscriptionWithCallback(
                        EMAIL,
                        PHONE,
                        TrackingConsent.ACCEPTED,
                        NOOP,
                    )
                },
                { AttentiveSdk.optUserOutOfMarketingSubscriptionWithCallback(EMAIL, PHONE, NOOP) },
            )
        assertEquals(4, callbackShapes.size)
    }

    // --- helpers -----------------------------------------------------------------------------

    private fun sendOptIn(consent: TrackingConsent) {
        runBlocking {
            val result = api.sendOptInSubscriptionStatus(PHONE, EMAIL, PUSH_TOKEN, consent)
            assertTrue("opt-in failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        }
    }

    private fun body() = JsonParser.parseString(bodies.single()).asJsonObject

    private fun recordingClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    val buffer = Buffer()
                    request.body?.writeTo(buffer)
                    bodies.add(buffer.readUtf8())
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("".toResponseBody(null))
                        .build()
                },
            )
            .build()

    private companion object {
        val CONFIG_FIELD =
            AttentiveEventTracker::class.java
                .getDeclaredField("config")
                .apply { isAccessible = true }

        val NOOP =
            object : AttentiveSdk.AttentiveCallback {
                override fun onSuccess() = Unit

                override fun onFailure(exception: Exception) = Unit
            }

        const val DOMAIN = "someDomain"
        const val VISITOR_ID = "someVisitorId"
        const val EMAIL = "shopper@example.com"
        const val PHONE = "+15551234567"
        const val PUSH_TOKEN = "somePushToken"
    }
}
