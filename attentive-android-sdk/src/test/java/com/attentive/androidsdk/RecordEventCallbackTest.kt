package com.attentive.androidsdk

import com.attentive.androidsdk.events.CustomEvent
import com.attentive.androidsdk.events.Event
import com.attentive.androidsdk.internal.network.ApiVersion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for the callback handling fix that prevents "Already resumed" crashes
 * when callbacks are invoked multiple times.
 *
 * This tests the fix in AttentiveEventTracker.recordEventSuspend where we added
 * continuation.isActive checks to prevent resuming an already-resumed continuation.
 */
class RecordEventCallbackTest {
    private lateinit var mockConfig: AttentiveConfig
    private lateinit var mockApi: AttentiveApi

    @Before
    fun setup() {
        mockConfig = mock()
        mockApi = mock()

        whenever(mockConfig.attentiveApi).thenReturn(mockApi)
        whenever(mockConfig.domain).thenReturn("test-domain")
        whenever(mockConfig.userIdentifiers).thenReturn(
            UserIdentifiers.Builder()
                .withVisitorId("test-visitor-id")
                .build(),
        )
        whenever(mockConfig.apiVersion).thenReturn(ApiVersion.OLD)
    }

    @Test
    fun oldApi_bothCallbacksInvoked_doesNotCrash() =
        runBlocking {
            val tracker = createTrackerWithConfig(mockConfig)

            doAnswer { invocation ->
                val callback = invocation.getArgument<AttentiveApiCallback>(3)
                callback.onSuccess()
                callback.onFailure("Some error")
                null
            }.whenever(mockApi).sendEvent(
                any<Event>(),
                any<UserIdentifiers>(),
                eq("test-domain"),
                any(),
            )

            val event = CustomEvent("test-event", emptyMap())

            val result = tracker.recordEventSuspend(event)
            assertTrue("First callback (success) should win", result.isSuccess)
        }

    @Test
    fun oldApi_onlyFailureCalled_returnsFailure() =
        runBlocking {
            val tracker = createTrackerWithConfig(mockConfig)

            doAnswer { invocation ->
                val callback = invocation.getArgument<AttentiveApiCallback>(3)
                callback.onFailure("Network error")
                null
            }.whenever(mockApi).sendEvent(
                any<Event>(),
                any<UserIdentifiers>(),
                eq("test-domain"),
                any(),
            )

            val event = CustomEvent("test-event", emptyMap())

            val result = tracker.recordEventSuspend(event)

            assertTrue("Should return failure when callback reports error", result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
            verify(mockApi, times(1)).sendEvent(any(), any(), any(), any())
        }

    @Test
    fun oldApi_onlySuccessCalled_returnsSuccess() =
        runBlocking {
            val tracker = createTrackerWithConfig(mockConfig)

            doAnswer { invocation ->
                val callback = invocation.getArgument<AttentiveApiCallback>(3)
                callback.onSuccess()
                null
            }.whenever(mockApi).sendEvent(
                any<Event>(),
                any<UserIdentifiers>(),
                eq("test-domain"),
                any(),
            )

            val event = CustomEvent("test-event", emptyMap())

            val result = tracker.recordEventSuspend(event)

            assertTrue("Should return success when callback reports success", result.isSuccess)
            verify(mockApi, times(1)).sendEvent(any(), any(), any(), any())
        }

    @Test
    fun oldApi_callbackInvokedMultipleTimes_onlyResumesOnce() =
        runBlocking {
            val tracker = createTrackerWithConfig(mockConfig)
            val callbackInvokeCount = AtomicInteger(0)

            doAnswer { invocation ->
                val callback = invocation.getArgument<AttentiveApiCallback>(3)
                callback.onSuccess()
                callbackInvokeCount.incrementAndGet()
                callback.onFailure("error 1")
                callbackInvokeCount.incrementAndGet()
                callback.onFailure("error 2")
                callbackInvokeCount.incrementAndGet()
                null
            }.whenever(mockApi).sendEvent(
                any<Event>(),
                any<UserIdentifiers>(),
                eq("test-domain"),
                any(),
            )

            val event = CustomEvent("test-event", emptyMap())

            val result = tracker.recordEventSuspend(event)

            assertEquals(3, callbackInvokeCount.get())
            assertTrue("First callback (success) should win", result.isSuccess)
        }

    @Test
    fun oldApi_notInitialized_returnsFailure() =
        runBlocking {
            val tracker =
                AttentiveEventTracker::class.java.getDeclaredConstructor().apply {
                    isAccessible = true
                }.newInstance()

            val event = CustomEvent("test-event", emptyMap())

            val result = tracker.recordEventSuspend(event)

            assertTrue("Should fail when not initialized", result.isFailure)
            assertTrue(
                result.exceptionOrNull() is IllegalStateException,
            )
        }

    /**
     * Helper to create an AttentiveEventTracker instance with the given config
     * without going through the singleton initialization
     */
    private fun createTrackerWithConfig(config: AttentiveConfig): AttentiveEventTracker {
        val tracker =
            AttentiveEventTracker::class.java.getDeclaredConstructor().apply {
                isAccessible = true
            }.newInstance()

        // Use reflection to set the config field directly
        val configField =
            AttentiveEventTracker::class.java.getDeclaredField("config").apply {
                isAccessible = true
            }
        configField.set(tracker, config)

        return tracker
    }
}
