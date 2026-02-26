package com.attentive.bonni.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation.NavHostController
import com.attentive.androidsdk.inbox.AttentiveInbox
import com.attentive.bonni.R
import com.attentive.bonni.SimpleToolbar
import com.attentive.bonni.ui.theme.BonniPink

@Composable
fun InboxScreen(navHostController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize()) {
        SimpleToolbar(
            title = "Inbox",
            navController = navHostController,
        )

        AttentiveInbox(
            backgroundColor = Color.White,
            unreadIndicatorColor = BonniPink,
            titleTextColor = colorResource(id = R.color.attentive_black),
            bodyTextColor = Color.DarkGray,
            timestampTextColor = Color.Gray,
            swipeBackgroundColor = BonniPink,
            titleFontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            bodyFontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
            timestampFontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        )
    }
}
