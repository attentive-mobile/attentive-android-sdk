package com.attentive.example2.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.creatives.Creative
import com.attentive.example2.BonniApp
import com.attentive.example2.R
import com.attentive.example2.SimpleToolbar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun SettingsScreen(navHostController: NavHostController) {
    SettingsScreenContent(navHostController)
}

@Composable
fun SettingsScreenContent(navHostController: NavHostController) {
    val activity = LocalActivity.current

    // Create the FrameLayout first
    val frameLayout = remember {
        FrameLayout(activity!!.baseContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Remember the Creative instance
    val creative = remember {
        Creative(AttentiveEventTracker.instance.config!!, frameLayout, activity)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SimpleToolbar(title = "Debug Screen", {}, navHostController)
        SettingsList(creative, navHostController)
    }
}

@Composable
fun SettingsList(creative: Creative, navHostController: NavHostController) {
    val accountSettings = mutableListOf<Pair<String, () -> Unit>>()
    accountSettings.add("Switch Account" to {})
    accountSettings.add("Manage Addresses" to {})

    val debugSettings = mutableListOf<Pair<String, () -> Unit>>()
    debugSettings.add("Debug" to { navHostController.navigate("DebugScreen") })

    val creativeSettings = mutableListOf<Pair<String, () -> Unit>>()
    creativeSettings.add("Show Creatives" to { creative.trigger() })
    creativeSettings.add("Identify Users" to {})
    creativeSettings.add("Clear Users" to {})

    val pushSettings = mutableListOf<Pair<String, () -> Unit>>()
    pushSettings.add("Display current push token" to {
        CoroutineScope(Dispatchers.IO).launch {
            getCurrentToken()
        }
    })
    val context = LocalContext.current
    pushSettings.add("Share push token" to {
        CoroutineScope(Dispatchers.Main).launch {
            sharePushToken(context)
        }
    })
    pushSettings.add("Send push token" to { sendPushToken(context) })
    Text(
        "Settings",
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Start,
        fontSize = 20.sp
    )
    Column() {
        SettingGroup(accountSettings)
        SettingGroup(debugSettings)
        SettingGroup(creativeSettings)
        SettingGroup(pushSettings)
        FeatureThatRequiresPushPermission()
    }
}

suspend fun getCurrentToken() {
    val context: Context = BonniApp.getInstance()
    AttentiveEventTracker.instance.getPushToken(requestPermission = false).let {
        if (it.isSuccess) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Push token: ${it.getOrNull()?.token}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

suspend fun sharePushToken(context: Context) {
    AttentiveEventTracker.instance.getPushToken(requestPermission = false).let {
        if (it.isSuccess) {
            val token = it.getOrNull()?.token
            if (token != null) {
                // Create a share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, token)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Add this flag
                }

                // Start the share intent
                context.startActivity(Intent.createChooser(shareIntent, "Share Push Token"))

                // Notify the user
                Toast.makeText(BonniApp.getInstance(), "Sharing push token...", Toast.LENGTH_SHORT)
                    .show()
                Timber.d("Push token shared: $token")
            } else {
                Toast.makeText(context, "Failed to fetch push token", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Failed to fetch push token", Toast.LENGTH_SHORT).show()
        }
    }
}

fun sendPushToken(context: Context) {
    AttentiveEventTracker.instance.registerPushToken(context)
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
fun SettingGroupInvokableComposable(
    titlesToDestinations: List<Pair<String, @Composable () -> Unit>>,
) {
    Column() {
        for (titleToDestination in titlesToDestinations) {
            SettingInvokableComposable(
                title = titleToDestination.first,
                content = titleToDestination.second
            )
        }
    }
    HorizontalLine(color = Color.Black, thickness = 2.dp, Modifier.padding(4.dp))
}


@Composable
fun Setting(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() }
    )
}

@Composable
fun SettingInvokableComposable(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable { /* Handle click if needed */ }
    ) {
        Text(text = title)
        content()
    }
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun FeatureThatRequiresPushPermission() {
    val context = LocalContext.current

    // Camera permission state
    val pushPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    if (pushPermissionState.status.isGranted) {
        Column {
            Text(
                "Push permission Granted",
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier = Modifier
                    .padding(8.dp)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(
                    "Revoke push permission after app restart",
                    fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            context.revokeSelfPermissionOnKill(Manifest.permission.POST_NOTIFICATIONS)
                        }
                )
            }
        }
    } else {
        Column {
            val textToShow = "Request push permission"
            Text(
                text = textToShow,
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        CoroutineScope(Dispatchers.Main).launch {
                            AttentiveEventTracker.instance.getPushToken(requestPermission = true)
                        }
                    }
            )
        }
    }
}