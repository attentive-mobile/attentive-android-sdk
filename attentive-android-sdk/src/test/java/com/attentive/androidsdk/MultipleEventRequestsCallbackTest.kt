package com.attentive.androidsdk

import com.attentive.androidsdk.events.AddToCartEvent
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Price
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Currency
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for the AttentiveApi fix that ensures when multiple event requests are sent,
 * the callback is only invoked once (preventing "Already resumed" crashes).
 */
class MultipleEventRequestsCallbackTest {
    private lateinit var attentiveApi: AttentiveApi
    private lateinit var okHttpClient: OkHttpClient
    private val callCaptor = ArgumentCaptor.forClass(Request::class.java)

    @Before
    fun setup() {
        okHttpClient = Mockito.mock(OkHttpClient::class.java)
        attentiveApi = Mockito.spy(AttentiveApi(okHttpClient, "test"))
    }

    @Test
    fun sendEvent_multipleRequests_callbackOnSuccessOnlyOnce() {
        // Arrange - Setup to return geo-adjusted domain
        setupGeoAdjustedDomainSuccess()

        // Mock OkHttp to succeed for all requests
        setupOkHttpSuccessForAllRequests()

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val latch = CountDownLatch(1)

        val callback = object : AttentiveApiCallback {
            override fun onSuccess() {
                successCount.incrementAndGet()
                latch.countDown()
            }

            override fun onFailure(message: String?) {
                failureCount.incrementAndGet()
                latch.countDown()
            }
        }

        // Create an event that generates multiple requests
        val event = createAddToCartEventWithMultipleItems()
        val userIdentifiers = UserIdentifiers.Builder()
            .withVisitorId("test-visitor-id")
            .build()

        // Act
        attentiveApi.sendEvent(event, userIdentifiers, "test-domain", callback)

        // Wait for async operations
        latch.await(5, TimeUnit.SECONDS)

        // Assert - callback.onSuccess() should be called exactly once
        // even though multiple HTTP requests were made
        assertEquals("onSuccess should be called exactly once", 1, successCount.get())
        assertEquals("onFailure should not be called", 0, failureCount.get())
    }

    @Test
    fun sendEvent_multipleRequestsOneFailure_callbackOnFailureOnlyOnce() {
        // Arrange
        setupGeoAdjustedDomainSuccess()

        // Mock OkHttp to fail on second request
        setupOkHttpWithOneFailure()

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val latch = CountDownLatch(1)

        val callback = object : AttentiveApiCallback {
            override fun onSuccess() {
                successCount.incrementAndGet()
                latch.countDown()
            }

            override fun onFailure(message: String?) {
                failureCount.incrementAndGet()
                latch.countDown()
            }
        }

        val event = createAddToCartEventWithMultipleItems()
        val userIdentifiers = UserIdentifiers.Builder()
            .withVisitorId("test-visitor-id")
            .build()

        // Act
        attentiveApi.sendEvent(event, userIdentifiers, "test-domain", callback)

        // Wait for async operations
        latch.await(5, TimeUnit.SECONDS)

        // Assert - callback.onFailure() should be called exactly once
        // even though multiple requests failed
        assertEquals("onFailure should be called exactly once", 1, failureCount.get())
        assertEquals("onSuccess should not be called", 0, successCount.get())
    }

    @Test
    fun sendEvent_multipleRequestsAllFail_callbackOnFailureOnlyOnce() {
        // Arrange
        setupGeoAdjustedDomainSuccess()

        // Mock OkHttp to fail for all requests
        setupOkHttpFailureForAllRequests()

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val latch = CountDownLatch(1)

        val callback = object : AttentiveApiCallback {
            override fun onSuccess() {
                successCount.incrementAndGet()
                latch.countDown()
            }

            override fun onFailure(message: String?) {
                failureCount.incrementAndGet()
                latch.countDown()
            }
        }

        val event = createAddToCartEventWithMultipleItems()
        val userIdentifiers = UserIdentifiers.Builder()
            .withVisitorId("test-visitor-id")
            .build()

        // Act
        attentiveApi.sendEvent(event, userIdentifiers, "test-domain", callback)

        // Wait for async operations
        latch.await(5, TimeUnit.SECONDS)

        // Assert
        assertEquals("onFailure should be called exactly once", 1, failureCount.get())
        assertEquals("onSuccess should not be called", 0, successCount.get())
    }

    @Test
    fun sendEvent_nullCallback_doesNotCrash() {
        // Arrange
        setupGeoAdjustedDomainSuccess()
        setupOkHttpSuccessForAllRequests()

        val event = createAddToCartEventWithMultipleItems()
        val userIdentifiers = UserIdentifiers.Builder()
            .withVisitorId("test-visitor-id")
            .build()

        // Act - Should not crash with null callback
        attentiveApi.sendEvent(event, userIdentifiers, "test-domain", null)

        // Wait a bit for async operations
        Thread.sleep(1000)

        // Assert - just verify it didn't crash
        assertTrue("Execution completed without crash", true)
    }

    // Helper methods

    private fun setupGeoAdjustedDomainSuccess() {
        doAnswer { invocation: InvocationOnMock ->
            val callback = invocation.getArgument(
                1,
                AttentiveApi.GetGeoAdjustedDomainCallback::class.java
            )
            callback.onSuccess("test-domain-geo")
            null
        }.whenever(attentiveApi).getGeoAdjustedDomainAsync(
            eq("test-domain"),
            any()
        )
    }

    private fun setupOkHttpSuccessForAllRequests() {
        val mockCall = mock<Call>()
        whenever(okHttpClient.newCall(any())).thenReturn(mockCall)

        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            val response = buildSuccessfulResponse()
            callback.onResponse(mockCall, response)
            null
        }.whenever(mockCall).enqueue(any())
    }

    private fun setupOkHttpWithOneFailure() {
        val mockCall = mock<Call>()
        whenever(okHttpClient.newCall(any())).thenReturn(mockCall)

        val callCount = AtomicInteger(0)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            val count = callCount.incrementAndGet()

            if (count == 1) {
                // First request succeeds
                val response = buildSuccessfulResponse()
                callback.onResponse(mockCall, response)
            } else {
                // Second and subsequent requests fail
                callback.onFailure(mockCall, java.io.IOException("Network error"))
            }
            null
        }.whenever(mockCall).enqueue(any())
    }

    private fun setupOkHttpFailureForAllRequests() {
        val mockCall = mock<Call>()
        whenever(okHttpClient.newCall(any())).thenReturn(mockCall)

        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onFailure(mockCall, java.io.IOException("Network error"))
            null
        }.whenever(mockCall).enqueue(any())
    }

    private fun buildSuccessfulResponse(): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://test.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody())
            .build()
    }

    private fun createAddToCartEventWithMultipleItems(): AddToCartEvent {
        // Create an AddToCart event with multiple items, which will generate
        // multiple EventRequest objects
        val items = listOf(
            Item(
                productId = "item1",
                productVariantId = "variant1",
                price = Price(
                    BigDecimal("10.00"),
                    Currency.getInstance("USD")
                ),
                name = "Test Item 1"
            ),
            Item(
                productId = "item2",
                productVariantId = "variant2",
                price = Price(
                    BigDecimal("20.00"),
                    Currency.getInstance("USD")
                ),
                name = "Test Item 2"
            )
        )

        return AddToCartEvent(items, deeplink = null)
    }
}
