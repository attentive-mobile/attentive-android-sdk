package com.attentive.example2.settings.debug

import android.R
import android.provider.CalendarContract.Colors
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.creatives.Creative
import com.attentive.example2.SimpleToolbar
import com.attentive.example2.settings.AdvertisementView
import com.attentive.example2.ui.theme.BonniPink
import com.attentive.example2.ui.theme.BonniYellow


@Composable
fun DebugScreenComposables(navHostController: NavHostController) {
    DebugScreenContent(navHostController)
}

@Composable
fun DebugScreenContent(navHostController: NavHostController) {
    val activity = LocalActivity.current
    val context = LocalContext.current

    // Create the FrameLayout once
    val frameLayout = remember {
        FrameLayout(activity!!.baseContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Configure AttentiveConfig once
    val config = remember {
        AttentiveConfig.Builder()
            .domain("76ers")
            .mode(AttentiveConfig.Mode.DEBUG)
            .context(context)
            .build()
    }
    AttentiveEventTracker.instance.config = config

    // Create the Creative instance once
    val creative = remember(frameLayout, activity) {
        Creative(AttentiveEventTracker.instance.config!!, frameLayout, activity)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SimpleToolbar("Debug Screen", {}, navHostController)
        Button(colors = ButtonDefaults.buttonColors(containerColor = BonniYellow), onClick = {
            creative.trigger() // Call the trigger method
        }) {
            Text("Trigger creative", color = Color.Black)
        }

        AdvertisementView(frameLayout)
    }
}


@Composable
fun AdvertisementView(frameLayout: FrameLayout) {
    AndroidView(factory = { frameLayout })
}