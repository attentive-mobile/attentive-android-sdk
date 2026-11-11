package com.attentive.androidsdk.internal.screens

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.attentive.androidsdk.screens.AttentiveScreen
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ScreenNavigationListenerTest {
    private val controller = mock<NavController>()
    private val reported = mutableListOf<AttentiveScreen>()
    private val listener = ScreenNavigationListener(null) { reported += it }

    @Test
    fun onDestinationChanged_newEntry_reportsScreen() {
        navigate("entry-1", "ProductScreenRoute")

        assertEquals(listOf(AttentiveScreen("product", "ProductScreenRoute")), reported)
    }

    @Test
    fun onDestinationChanged_sameEntryAgain_reportsOnce() {
        navigate("entry-1", "ProductScreenRoute")
        navigate("entry-1", "ProductScreenRoute")

        assertEquals(1, reported.size)
    }

    @Test
    fun onDestinationChanged_newEntryForSameRoute_reportsAgain() {
        navigate("entry-1", "product/{id}")
        navigate("entry-2", "product/{id}")

        assertEquals(2, reported.size)
    }

    @Test
    fun onDestinationChanged_unresolvedDestination_reportsNothing() {
        navigate("entry-1", null)

        assertEquals(emptyList<AttentiveScreen>(), reported)
    }

    private fun navigate(
        entryId: String,
        route: String?,
    ) {
        val entry = mock<NavBackStackEntry>()
        whenever(entry.id).thenReturn(entryId)
        whenever(controller.currentBackStackEntry).thenReturn(entry)
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn(route)

        listener.onDestinationChanged(controller, destination, null)
    }
}
