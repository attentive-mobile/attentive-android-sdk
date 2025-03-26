package com.attentive.example2.debug

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.creatives.Creative
import com.attentive.example2.SimpleToolbar

@Composable
fun DebugScreen(navHostController: NavHostController){
    DebugScreenContent(navHostController)
}

@Composable
fun DebugScreenContent(navHostController: NavHostController) {
    val activity = LocalActivity.current

    // Create the FrameLayout first
    val frameLayout = remember {
        FrameLayout(activity!!.baseContext).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    // Remember the Creative instance
    val creative = remember {
        Creative(AttentiveEventTracker.instance.config!!, frameLayout, activity)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally){
        SimpleToolbar(title = "Debug Screen",{}, navHostController)
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