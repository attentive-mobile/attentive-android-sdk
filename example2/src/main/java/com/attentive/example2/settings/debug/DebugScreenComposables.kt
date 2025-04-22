package com.attentive.example2.settings.debug

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.creatives.Creative
import com.attentive.example2.SimpleToolbar
import com.attentive.example2.settings.AdvertisementView


@Composable
fun DebugScreenComposables(navHostController: NavHostController){
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
            .domain("games")
            .mode(AttentiveConfig.Mode.DEBUG)
            .context(context)
            .build()
    }
    AttentiveEventTracker.instance.config = config

    // Create the Creative instance once
    val creative = remember(frameLayout, activity) {
        Creative(AttentiveEventTracker.instance.config!!, frameLayout, activity)
    }

    Column {
        SimpleToolbar("Debug Screen", {}, navHostController)
        Button(onClick = {
            creative.trigger() // Call the trigger method
        }) {
            Text("Trigger creative")
        }

        AdvertisementView(frameLayout)
    }
}


@Composable
fun AdvertisementView(frameLayout: FrameLayout) {
    AndroidView(factory = { frameLayout })
}