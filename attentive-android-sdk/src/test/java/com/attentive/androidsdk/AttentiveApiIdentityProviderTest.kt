package com.attentive.androidsdk

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test

/**
 * Covers MSDK-304: [AttentiveApi] takes its domain and identity state from an injected
 * [AttentiveIdentityProvider] instead of reaching back through
 * `AttentiveEventTracker.instance.config`.
 *
 * Every test here constructs [AttentiveApi] on its own, with no [AttentiveEventTracker]
 * initialization anywhere. That is the point: while [AttentiveApi] read the singleton, these
 * paths threw `UninitializedPropertyAccessException` on the tracker's `lateinit config`.
 */
class AttentiveApiIdentityProviderTest {
    @Test
    fun sendOptInSubscriptionStatus_buildsRequestFromIdentityProvider_withoutTrackerSingleton() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val identityProvider =
            FakeAttentiveIdentityProvider(
                domain = "provider-domain",
                userIdentifiers = UserIdentifiers.Builder().withVisitorId("provider-visitor").build(),
            )
        val api =
            AttentiveApi(
                buildInterceptorClient(capturedRequests),
                "constructor-domain",
                identityProvider,
            )

        // Act
        val result = runBlocking { api.sendOptInSubscriptionStatus("+15556667777", "a@b.com", "token") }

        // Assert
        Assert.assertTrue(result.isSuccess)
        Assert.assertEquals(1, capturedRequests.size)
        val captured = capturedRequests[0]
        Assert.assertTrue(captured.request.url.encodedPath.endsWith("/opt-in-subscriptions"))

        val body = JsonParser.parseString(captured.bodyJson).asJsonObject
        Assert.assertEquals("provider-domain", body.get("c").asString)
        Assert.assertEquals("provider-visitor", body.get("u").asString)
    }

    @Test
    fun sendOptOutSubscriptionStatus_readsVisitorIdFromIdentityProvider() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val identityProvider =
            FakeAttentiveIdentityProvider(
                userIdentifiers = UserIdentifiers.Builder().withVisitorId("opt-out-visitor").build(),
            )
        val api =
            AttentiveApi(buildInterceptorClient(capturedRequests), "games", identityProvider)

        // Act
        val result =
            runBlocking {
                api.sendOptOutSubscriptionStatus("a@b.com", "+15556667777", "explicit-domain", "token")
            }

        // Assert
        Assert.assertTrue(result.isSuccess)
        val body = JsonParser.parseString(capturedRequests.single().bodyJson).asJsonObject
        Assert.assertEquals("opt-out-visitor", body.get("u").asString)
        // Opt-out takes its domain as a parameter, so the caller's value still wins there.
        Assert.assertEquals("explicit-domain", body.get("c").asString)
    }

    /**
     * The domain is read from the provider on each call rather than captured at construction,
     * so a runtime `AttentiveConfig.changeDomain` is reflected — matching the behaviour of the
     * previous `AttentiveEventTracker.instance.config.domain` lookup.
     */
    @Test
    fun sendOptInSubscriptionStatus_picksUpDomainChangedAfterConstruction() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val identityProvider = FakeAttentiveIdentityProvider(domain = "before")
        val api =
            AttentiveApi(buildInterceptorClient(capturedRequests), "games", identityProvider)

        // Act
        identityProvider.domain = "after"
        runBlocking { api.sendOptInSubscriptionStatus("+15556667777", "a@b.com", "token") }

        // Assert
        val body = JsonParser.parseString(capturedRequests.single().bodyJson).asJsonObject
        Assert.assertEquals("after", body.get("c").asString)
    }

    @Test
    fun sendOptInSubscriptionStatus_failsWithoutVisitorId_fromIdentityProvider() {
        // Arrange — a provider with no visitor ID, as before the SDK has generated one
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val api =
            AttentiveApi(
                buildInterceptorClient(capturedRequests),
                "games",
                FakeAttentiveIdentityProvider(userIdentifiers = UserIdentifiers.Builder().build()),
            )

        // Act
        val result = runBlocking { api.sendOptInSubscriptionStatus("+15556667777", "a@b.com", "token") }

        // Assert
        Assert.assertTrue(result.isFailure)
        Assert.assertTrue(result.exceptionOrNull() is IllegalStateException)
        Assert.assertTrue(capturedRequests.isEmpty())
    }

    @Test
    fun sendUserUpdate_identifiesThroughIdentityProvider() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val identityProvider = FakeAttentiveIdentityProvider()
        val api =
            AttentiveApi(buildInterceptorClient(capturedRequests), "games", identityProvider)

        // Act
        val result =
            runBlocking {
                api.sendUserUpdate(
                    domain = "games",
                    email = "a@b.com",
                    phoneNumber = "+15556667777",
                    visitorId = "someVisitorId",
                    pushToken = "token",
                )
            }

        // Assert
        Assert.assertTrue(result.isSuccess)
        Assert.assertTrue(capturedRequests.single().request.url.encodedPath.endsWith("/user-update"))

        val identified = identityProvider.identifyCalls.single()
        Assert.assertEquals("a@b.com", identified.email)
        Assert.assertEquals("+15556667777", identified.phone)
    }

    @Test
    fun sendUserUpdate_withoutEmailOrPhone_doesNotIdentify() {
        // Arrange
        val capturedRequests = mutableListOf<CapturedApiRequest>()
        val identityProvider = FakeAttentiveIdentityProvider()
        val api =
            AttentiveApi(buildInterceptorClient(capturedRequests), "games", identityProvider)

        // Act
        runBlocking {
            api.sendUserUpdate(
                domain = "games",
                email = null,
                phoneNumber = null,
                visitorId = "someVisitorId",
                pushToken = "token",
            )
        }

        // Assert
        Assert.assertTrue(identityProvider.identifyCalls.isEmpty())
    }

    /**
     * [AttentiveConfig] is what gets injected in production, so it has to satisfy the
     * narrower contract.
     */
    @Test
    fun attentiveConfigInterface_isAnIdentityProvider() {
        Assert.assertTrue(
            AttentiveIdentityProvider::class.java.isAssignableFrom(AttentiveConfigInterface::class.java),
        )
    }

    private data class CapturedApiRequest(
        val request: Request,
        val bodyJson: String,
    )

    private fun buildInterceptorClient(capturedRequests: MutableList<CapturedApiRequest>): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host == AttentiveApi.ATTENTIVE_MOBILE_ENDPOINT_HOST) {
                    val buffer = okio.Buffer()
                    request.body?.writeTo(buffer)
                    capturedRequests.add(
                        CapturedApiRequest(request = request, bodyJson = buffer.readUtf8()),
                    )
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(204)
                    .message("No Content")
                    .body("".toResponseBody())
                    .build()
            }
            .build()
    }
}
