package com.attentive.bonni.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.attentive.androidsdk.inbox.AttentiveInboxView
import com.attentive.bonni.R
import com.attentive.bonni.SimpleToolbar
import com.attentive.bonni.ui.theme.BonniPink

/**
 * Legacy inbox screen that demonstrates using AttentiveInboxView (the View system wrapper)
 * within a Compose screen via AndroidView.
 *
 * This shows how the legacy View implementation works in a Compose context.
 */
@Composable
fun LegacyInboxScreen(
    navHostController: NavHostController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SimpleToolbar(
            title = "Inbox (Legacy View)",
            navController = navHostController
        )

        // Use AndroidView to bridge from Compose to the legacy View system
        AndroidView(
            factory = { context ->
                AttentiveInboxView(context).apply {
                    // Set colors programmatically
                    setInboxBackgroundColor(android.graphics.Color.WHITE)
                    setUnreadIndicatorColor(BonniPink.toArgb())
                    setTitleTextColor(context.getColor(R.color.attentive_black))
                    setBodyTextColor(android.graphics.Color.DKGRAY)
                    setTimestampTextColor(android.graphics.Color.GRAY)
                    setSwipeBackgroundColor(BonniPink.toArgb())

                    // Set fonts programmatically
                    setTitleFontFamily(R.font.degulardisplay_regular)
                    setBodyFontFamily(R.font.degulardisplay_regular)
                    setTimestampFontFamily(R.font.degulardisplay_regular)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
