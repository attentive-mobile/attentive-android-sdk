package com.attentive.androidsdk.screens

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.attentive.androidsdk.internal.screens.LoggingScreenViewReporter
import com.attentive.androidsdk.internal.screens.ScreenViewReporter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AttentiveScreenTrackingTest {
    private val reported = mutableListOf<AttentiveScreen>()

    @After
    fun tearDown() {
        AttentiveScreenTracking.reporter = LoggingScreenViewReporter()
    }

    @Test
    fun trackNavigation_destinationChanged_reportsScreen() {
        AttentiveScreenTracking.reporter = ScreenViewReporter { reported += it }
        val controller = mock<NavController>()
        AttentiveScreenTracking.trackNavigation(controller)

        val listener = argumentCaptor<NavController.OnDestinationChangedListener>()
        verify(controller).addOnDestinationChangedListener(listener.capture())
        val entry = mock<NavBackStackEntry>()
        whenever(entry.id).thenReturn("entry-1")
        whenever(controller.currentBackStackEntry).thenReturn(entry)
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("CartScreen")
        listener.firstValue.onDestinationChanged(controller, destination, null)

        assertEquals(listOf(AttentiveScreen("cart", "CartScreen")), reported)
        assertEquals(AttentiveScreen("cart", "CartScreen"), AttentiveScreenTracking.currentScreen.value)
    }
}
