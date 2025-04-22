package com.attentive.example2.settings

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.creatives.Creative
import com.attentive.example2.SimpleToolbar

@Composable
fun SettingsScreen(navHostController: NavHostController){
    SettingsScreenContent(navHostController)
}

@Composable
fun SettingsScreenContent(navHostController: NavHostController) {
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
        SimpleToolbar(title = "Debug Screen", {}, navHostController)
        SettingsList(navHostController)
        Button(onClick = {
            creative.trigger() // Call the trigger method
        }) {
            Text("Trigger creative")
        }

        AdvertisementView(frameLayout)
    }
}

@Composable
fun SettingsList(navHostController: NavHostController) {
    val accountSettings = mutableListOf<Pair<String, () -> Unit>>()
    accountSettings.add("Switch Account" to {})
    accountSettings.add("Manage Addresses" to {})

    val debugSettings = mutableListOf<Pair<String, () -> Unit>>()
    debugSettings.add("Debug" to {navHostController.navigate("DebugScreen")})
    debugSettings.add("Show Creatives" to {})
    debugSettings.add("Identify Users" to {})
    debugSettings.add("Clear Users" to {})
    Text("Settings", modifier = Modifier.padding(8.dp))
    Column() {
        SettingGroup(accountSettings)
        SettingGroup(debugSettings)
    }
}

@Composable
fun SettingGroup(
    titlesToDestinations: List<Pair<String, () -> Unit>>,
) {
    Column() {
        for (titleToDestination in titlesToDestinations) {
            Setting(title = titleToDestination.first, onClick = titleToDestination.second)
        }
    }
    HorizontalLine(color = Color.Black, thickness = 2.dp, Modifier.padding(4.dp))
}

@Composable
fun Setting(title: String, onClick: () -> Unit){
        Text(
            text = title,
            modifier = Modifier.padding(8.dp).clickable { onClick() }
        )
}

@Composable
fun HorizontalLine(
    color: Color = Color.Gray,
    thickness: Dp = Dp.Hairline,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = Dp.Hairline,
        color = color,
    )
}

@Composable
fun AdvertisementView(frameLayout: FrameLayout) {
    AndroidView(factory = { frameLayout })
}