package com.attentive.androidsdk.tracking

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.attentive.androidsdk.AttentiveApi
import com.attentive.androidsdk.AttentiveEventTracker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AppLaunchTrackerTest {

    private lateinit var mockApplication: Application
    private lateinit var mockLifecycle: Lifecycle
    private lateinit var mockEventTracker: AttentiveEventTracker
    private lateinit var activityCallback: Application.ActivityLifecycleCallbacks
    private lateinit var tracker: AppLaunchTracker

    @Before
    fun setup() {
        mockApplication = mock()
        mockLifecycle = mock()
        whenever(mockLifecycle.currentState).thenReturn(Lifecycle.State.INITIALIZED)

        mockEventTracker = mock()
        setAttentiveEventTrackerInstance(mockEventTracker)

        tracker = AppLaunchTracker(mockApplication, mockLifecycle)

        val captor = ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks::class.java)
        Mockito.verify(mockApplication).registerActivityLifecycleCallbacks(captor.capture())
        activityCallback = captor.value

        // Allow any leaked IO coroutines from prior tests to settle, then clear
        Thread.sleep(100)
        Mockito.clearInvocations(mockEventTracker)
    }

    @After
    fun teardown() {
        setAttentiveEventTrackerInstance(null)
    }

    private fun setAttentiveEventTrackerInstance(instance: AttentiveEventTracker?) {
        val field = AttentiveEventTracker::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, instance)
    }

    private fun getHasSentLaunchEvent(): Boolean {
        val field = AppLaunchTracker::class.java.getDeclaredField("hasSentLaunchEvent")
        field.isAccessible = true
        return field.getBoolean(tracker)
    }

    private fun createMockActivity(launchedFromNotification: Boolean): Activity {
        val activity: Activity = mock()
        val intent: Intent = mock()
        val extras: Bundle = mock()

        whenever(activity.intent).thenReturn(intent)
        whenever(intent.extras).thenReturn(extras)
        whenever(extras.getBoolean(AppLaunchTracker.LAUNCHED_FROM_NOTIFICATION, false))
            .thenReturn(launchedFromNotification)
        whenever(extras.keySet()).thenReturn(
            if (launchedFromNotification) setOf(AppLaunchTracker.LAUNCHED_FROM_NOTIFICATION)
            else emptySet()
        )

        return activity
    }

    // --- Guard behavior tests ---

    @Test
    fun `first onActivityResumed sets guard flag`() {
        activityCallback.onActivityResumed(createMockActivity(false))
        assertTrue("hasSentLaunchEvent should be true after first resume", getHasSentLaunchEvent())
    }

    @Test
    fun `duplicate onActivityResumed is blocked by guard`() {
        // First resume - normal launch
        activityCallback.onActivityResumed(createMockActivity(false))

        // Second resume with notification flag - should be blocked by guard
        activityCallback.onActivityResumed(createMockActivity(true))

        // DIRECT_OPEN should NOT be added because guard blocked the second resume
        assertFalse(
            "Guard should prevent processing on duplicate resume",
            tracker.launchEvents.contains(AttentiveApi.LaunchType.DIRECT_OPEN)
        )
    }

    @Test
    fun `process lifecycle onStop resets guard`() {
        activityCallback.onActivityResumed(createMockActivity(false))
        assertTrue(getHasSentLaunchEvent())

        tracker.onStop(mock<LifecycleOwner>())
        assertFalse("Guard should reset after process onStop", getHasSentLaunchEvent())
    }

    @Test
    fun `new foreground session processes event after guard reset`() {
        // First foreground session
        activityCallback.onActivityResumed(createMockActivity(false))
        assertTrue(getHasSentLaunchEvent())

        // Simulate app backgrounding
        tracker.onStop(mock<LifecycleOwner>())
        activityCallback.onActivityStopped(createMockActivity(false))

        // New foreground session with notification
        activityCallback.onActivityResumed(createMockActivity(true))
        assertTrue(getHasSentLaunchEvent())
        assertTrue(
            "Second session should process new events",
            tracker.launchEvents.contains(AttentiveApi.LaunchType.DIRECT_OPEN)
        )
    }

    // --- Event type tests ---

    @Test
    fun `notification intent adds DIRECT_OPEN to launch events`() {
        activityCallback.onActivityResumed(createMockActivity(true))
        assertTrue(tracker.launchEvents.contains(AttentiveApi.LaunchType.DIRECT_OPEN))
    }

    @Test
    fun `normal launch does not add DIRECT_OPEN`() {
        activityCallback.onActivityResumed(createMockActivity(false))
        assertFalse(tracker.launchEvents.contains(AttentiveApi.LaunchType.DIRECT_OPEN))
    }

    // --- Event sending verification ---

    @Test
    fun `normal launch sends APP_LAUNCHED event`() {
        activityCallback.onActivityResumed(createMockActivity(false))

        Thread.sleep(500)

        runBlocking {
            verify(mockEventTracker).sendAppLaunchEvent(
                eq(AttentiveApi.LaunchType.APP_LAUNCHED),
                any()
            )
        }
    }

    @Test
    fun `notification launch sends DIRECT_OPEN event`() {
        activityCallback.onActivityResumed(createMockActivity(true))

        Thread.sleep(500)

        runBlocking {
            verify(mockEventTracker).sendAppLaunchEvent(
                eq(AttentiveApi.LaunchType.DIRECT_OPEN),
                any()
            )
        }
    }

    @Test
    fun `duplicate resume sends only one event`() {
        activityCallback.onActivityResumed(createMockActivity(false))

        Thread.sleep(500)

        // Second resume should be blocked by guard
        activityCallback.onActivityResumed(createMockActivity(false))

        Thread.sleep(500)

        runBlocking {
            verify(mockEventTracker, Mockito.times(1)).sendAppLaunchEvent(any(), any())
        }
    }
}
