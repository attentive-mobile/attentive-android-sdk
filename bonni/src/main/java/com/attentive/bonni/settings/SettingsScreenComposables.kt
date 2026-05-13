package com.attentive.bonni.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.creatives.Creative
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.bonni.BonniApp
import com.attentive.bonni.BonniApp.Companion.ATTENTIVE_EMAIL_PREFS
import com.attentive.bonni.BonniApp.Companion.ATTENTIVE_PHONE_PREFS
import com.attentive.bonni.BonniApp.Companion.ATTENTIVE_PREFS
import com.attentive.bonni.R
import com.attentive.androidsdk.internal.util.AppInfo
import com.attentive.bonni.SimpleToolbar
import com.attentive.bonni.ui.theme.BonniPink
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class SettingItem(
    val title: String,
    val enabled: Boolean = true,
    val editable: Boolean = false,
    val onClick: (String) -> Unit = {},
    val invokableComposable: @Composable ((String) -> Unit)? = null,
)

@Composable
fun SettingsScreen(navHostController: NavHostController) {
    SettingsScreenContent(navHostController)
}

@Composable
fun SettingsScreenContent(navHostController: NavHostController) {
    val activity =
        requireNotNull(LocalActivity.current) {
            "Activity required for Creative initialization"
        }

    val frameLayout =
        remember {
            FrameLayout(activity.baseContext).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        }

    val creative =
        remember {
            Creative(AttentiveEventTracker.instance.config, frameLayout, activity)
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SimpleToolbar(title = "Settings", {}, navHostController)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            SettingsList(creative, navHostController)
            AndroidView(
                factory = { frameLayout },
                modifier =
                    Modifier
                        .fillMaxSize(),
            )
        }
    }
}

@Composable
fun SettingsList(
    creative: Creative,
    navHostController: NavHostController,
) {
    val viewModel: SettingsViewModel = viewModel()
    val accountSettings = mutableListOf<SettingItem>()
    val changeDomainSetting =
        SettingItem(
            title = "Change Domain",
            enabled = true,
            editable = true,
            onClick = { domain -> changeDomain(domain) },
        )

    val changeEmailSetting =
        SettingItem(
            title = "Email",
            enabled = true,
            editable = true,
            onClick = { email -> changeEmail(viewModel) },
        )

    val changePhoneNumberSetting =
        SettingItem(
            title = "Phone",
            enabled = true,
            editable = true,
            onClick = { phone -> changePhoneNumber(viewModel) },
        )

    val apiVersionSetting =
        SettingItem(
            title = "Toggle Api Version",
            enabled = true,
            editable = false,
            onClick = { viewModel.toggleEndpointVersion() },
        )

    accountSettings.add(changeDomainSetting)
    accountSettings.add(changeEmailSetting)

    val debugSettings = mutableListOf<Pair<String, () -> Unit>>()
    val currentApiPreference = viewModel.getEndpointVersion()

//    debugSettings.add("Toggle Api Version - Current: $apiVersionString" to { viewModel.toggleEndpointVersion() })

    val creativeSettings = mutableListOf<Pair<String, () -> Unit>>()
    creativeSettings.add("Show Creatives" to { creative.trigger() })
    creativeSettings.add(
        "Clear Cookies (ignore filtering rules for next creative)" to {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        },
    )

    val pushSettings = mutableListOf<Pair<String, () -> Unit>>()
    pushSettings.add(
        "Display current push token" to {
            CoroutineScope(Dispatchers.IO).launch {
                getCurrentToken()
            }
        },
    )

    val activity = LocalActivity.current
    val context = LocalContext.current

    pushSettings.add(
        "Share push token" to {
            CoroutineScope(Dispatchers.Main).launch {
                sharePushToken(activity!!)
            }
        },
    )

    pushSettings.add(
        "Update push permission status" to {
            AttentiveSdk.updatePushPermissionStatus(context)
            val granted = AttentiveSdk.isPushPermissionGranted(context)
            Toast.makeText(
                context,
                "Re-registered push token. Permission: ${if (granted) "granted" else "denied"}",
                Toast.LENGTH_SHORT,
            ).show()
        },
    )

    val deepLinkSettings = mutableListOf<Pair<String, () -> Unit>>()
    deepLinkSettings.add(
        "Trigger Cart Deep Link Notification" to {
            triggerMockDeepLinkNotification(
                context,
                withDeepLink = true,
            )
        },
    )
    deepLinkSettings.add(
        "Trigger No Deep Link Notification" to {
            triggerMockDeepLinkNotification(
                context,
                withDeepLink = false,
            )
        },
    )

    val lifecycleSettings = mutableListOf<Pair<String, () -> Unit>>()
    lifecycleSettings.add(
        "Login current user" to {
            val savedEmail = viewModel.saveEmail()
            val savedPhone = viewModel.savePhoneNumber()
            val email = viewModel.email.value.takeIf { it.isNotBlank() && savedEmail }
            val phone = viewModel.phone.value.takeIf { it.isNotBlank() && savedPhone }
            val identifier = listOfNotNull(email, phone).joinToString(", ")
            val message = if (identifier.isBlank()) {
                "No valid email or phone to login"
            } else {
                "Logged in current user: $identifier"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )
    lifecycleSettings.add(
        "Log in as different user" to {
            viewModel.saveEmail()
            viewModel.savePhoneNumber()
            viewModel.switchUser()
            val email = viewModel.email.value.takeIf { it.isNotBlank() }
            val phone = viewModel.phone.value.takeIf { it.isNotBlank() }
            val identifier = listOfNotNull(email, phone).joinToString(", ")
            val message = if (identifier.isBlank()) {
                "No email or phone set, can't switch user"
            } else {
                "Logged in as $identifier"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )
    lifecycleSettings.add("Log out" to { clearUsers(viewModel) })

    val subscriptionSettings = mutableListOf<Pair<String, () -> Unit>>()
    subscriptionSettings.add(
        "Subscribe email" to {
            CoroutineScope(Dispatchers.IO).launch {
                val email = AttentiveEventTracker.instance.config.userIdentifiers.email
                if (email == null) {
                    showOnMain(context, "No email, can't subscribe")
                    return@launch
                }
                val result = AttentiveSdk.optUserIntoMarketingSubscription(email = email)
                showOnMain(context, subscriptionResultMessage(result, "Subscribed email", email))
            }
        },
    )

    subscriptionSettings.add(
        "Unsubscribe email" to {
            CoroutineScope(Dispatchers.IO).launch {
                val email = AttentiveEventTracker.instance.config.userIdentifiers.email
                if (email == null) {
                    showOnMain(context, "No email, can't unsubscribe")
                    return@launch
                }
                val result = AttentiveSdk.optUserOutOfMarketingSubscription(email = email)
                showOnMain(context, subscriptionResultMessage(result, "Unsubscribed email", email))
            }
        },
    )

    subscriptionSettings.add(
        "Subscribe SMS" to {
            CoroutineScope(Dispatchers.IO).launch {
                val phone = AttentiveEventTracker.instance.config.userIdentifiers.phone
                if (phone == null) {
                    showOnMain(context, "No number, can't subscribe")
                    return@launch
                }
                val result = AttentiveSdk.optUserIntoMarketingSubscription(phoneNumber = phone)
                showOnMain(context, subscriptionResultMessage(result, "Subscribed SMS", phone))
            }
        },
    )

    subscriptionSettings.add(
        "Unsubscribe SMS" to {
            CoroutineScope(Dispatchers.IO).launch {
                val phone = AttentiveEventTracker.instance.config.userIdentifiers.phone
                if (phone == null) {
                    showOnMain(context, "No number, can't unsubscribe")
                    return@launch
                }
                val result = AttentiveSdk.optUserOutOfMarketingSubscription(phoneNumber = phone)
                showOnMain(context, subscriptionResultMessage(result, "Unsubscribed SMS", phone))
            }
        },
    )

    LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
        items(count = 1) {
            SectionHeader("Configuration")
            EditableDomainSetting(changeDomainSetting)
            SectionHeader("User")
            EditableEmailSetting(changeEmailSetting)
            EditablePhoneNumberSetting(changePhoneNumberSetting)
            SettingGroup(lifecycleSettings)
            SectionHeader("Marketing subscriptions")
            SettingGroup(subscriptionSettings)
            SectionHeader("Debug")
            ApiVersionSetting(apiVersionSetting)
            SectionHeader("Creatives")
            SettingGroup(creativeSettings)
            SectionHeader("Push notifications")
            PushPermissionRequest()
            SettingGroup(pushSettings)
            SettingGroup(deepLinkSettings)
            AboutSection()
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    HorizontalLine(color = Color.Black, Modifier.padding(horizontal = 4.dp))
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = BonniPink,
        fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        modifier =
            Modifier
                .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)
                .fillMaxWidth(),
    )
}

@Composable
fun AboutSection() {
    val context = LocalContext.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val appVersion = packageInfo.versionName ?: "?"
    val buildNumber =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }
    val sdkVersion = AppInfo.attentiveSDKVersion

    SectionHeader("About")
    Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
        Text(
            "Bonni $appVersion ($buildNumber)",
            fontSize = 12.sp,
            color = Color.Gray,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        )
        Text(
            "Attentive SDK $sdkVersion",
            fontSize = 12.sp,
            color = Color.Gray,
            fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        )
    }
}

@Composable
fun EditableDomainSetting(settingItem: SettingItem) {
    var isEditing by remember { mutableStateOf(false) }
    var domain by remember { mutableStateOf(AttentiveEventTracker.instance.config.domain) }

    AnimatedContent(targetState = isEditing) { editing ->
        if (editing) {
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text(settingItem.title) },
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BonniPink,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.Black,
                        ),
                    trailingIcon = {
                        IconButton(onClick = {
                            settingItem.onClick(domain)
                            isEditing = false
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Submit",
                                tint = BonniPink,
                            )
                        }
                    },
                )
            }
        } else {
            Text(
                text =
                    buildAnnotatedString {
                        append("Change current domain: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(domain)
                        }
                    },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier =
                    Modifier
                        .padding(8.dp)
                        .clickable { isEditing = true },
            )
        }
    }
}

@Composable
fun EditableEmailSetting(
    settingItem: SettingItem,
    viewModel: SettingsViewModel = viewModel(),
) {
    var isEditing by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    val email by viewModel.email.collectAsState()
    val context = LocalContext.current

    AnimatedContent(targetState = isEditing) { editing ->
        if (editing) {
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        viewModel.updateEmail(it)
                        isError = false
                    },
                    label = { Text(settingItem.title) },
                    supportingText = if (isError) {
                        { Text("Please enter a valid email address") }
                    } else {
                        null
                    },
                    isError = isError,
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BonniPink,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.Black,
                        ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (viewModel.saveEmail()) {
                                isError = false
                                isEditing = false
                                Toast.makeText(
                                    context,
                                    "Logged in current user: ${viewModel.email.value}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                isError = true
                            }
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Submit",
                                tint = BonniPink,
                            )
                        }
                    },
                )
            }
        } else {
            Text(
                text =
                    buildAnnotatedString {
                        append("Email: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(email)
                        }
                    },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier =
                    Modifier
                        .padding(8.dp)
                        .clickable { isEditing = true },
            )
        }
    }
}

@Composable
fun EditablePhoneNumberSetting(
    settingItem: SettingItem,
    viewModel: SettingsViewModel = viewModel(),
) {
    var isEditing by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    val phone by viewModel.phone.collectAsState()
    val context = LocalContext.current

    fun attemptSave(): Boolean {
        return if (viewModel.savePhoneNumber()) {
            isError = false
            isEditing = false
            Toast.makeText(
                context,
                "Logged in current user: ${viewModel.phone.value}",
                Toast.LENGTH_SHORT,
            ).show()
            true
        } else {
            isError = true
            false
        }
    }

    AnimatedContent(targetState = isEditing) { editing ->
        if (editing) {
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        viewModel.updatePhone(it)
                        isError = false
                    },
                    label = { Text(settingItem.title) },
                    supportingText = if (isError) {
                        { Text("Please enter a valid phone number (E.164 format, e.g. +14155551234)") }
                    } else {
                        null
                    },
                    isError = isError,
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BonniPink,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.Black,
                        ),
                    trailingIcon = {
                        IconButton(onClick = { attemptSave() }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Submit",
                                tint = BonniPink,
                            )
                        }
                    },
                )
            }
        } else {
            Text(
                text =
                    buildAnnotatedString {
                        append("Phone: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(phone)
                        }
                    },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier =
                    Modifier
                        .padding(8.dp)
                        .clickable { isEditing = true },
            )
        }
    }
}

@Composable
fun ApiVersionSetting(
    settingItem: SettingItem,
    viewModel: SettingsViewModel = viewModel(),
) {
    val endpointVersion by viewModel.endpointVersion.collectAsState()
    Setting(
        title = endpointVersion,
        enabled = settingItem.enabled,
        onClick = { settingItem.onClick("") },
    )
}

suspend fun getCurrentToken() {
    val context = BonniApp.getInstance()
    AttentiveSdk.getPushToken(context, requestPermission = false).let {
        if (it.isSuccess) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Push token: ${it.getOrNull()?.token}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

fun triggerMockDeepLinkNotification(
    context: Context,
    withDeepLink: Boolean,
) {
    Timber.d("Triggering mock notification with deep link: $withDeepLink")
    var dataMap: MutableMap<String, String>
    if (withDeepLink) {
        dataMap = mutableMapOf("attentive_open_action_url" to "bonni://cart")
    } else {
        dataMap = mutableMapOf("attentive_open_action_url" to "")
    }

    val title = "Bonni Cart"
    val body = "Your cart is \"waiting\" for you!"

    dataMap[Constants.Companion.KEY_NOTIFICATION_TITLE] = title
    dataMap[Constants.Companion.KEY_NOTIFICATION_BODY] = body

    if (AttentiveSdk.isPushPermissionGranted(context).not()) {
        Timber.w("Push permission not granted, cannot send mock notification")
        Toast.makeText(context, "Push permission not granted", Toast.LENGTH_SHORT).show()
        return
    } else {
        AttentiveSdk.sendMockNotification(
            title,
            body,
            dataMap,
            R.drawable.bonni_logo,
            BonniApp.getInstance(),
        )
    }
}

fun clearUsers(viewModel: SettingsViewModel) {
    Timber.d("Clearing users")
    viewModel.clearPhone()
    viewModel.clearEmail()
    AttentiveSdk.clearUser()
    BonniApp
        .getInstance()
        .getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        .edit {
            remove(ATTENTIVE_EMAIL_PREFS)
            remove(ATTENTIVE_PHONE_PREFS)
        }
    Toast.makeText(BonniApp.getInstance(), "Logged out", Toast.LENGTH_SHORT).show()
}

fun changeDomain(domain: String) {
    BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        .edit {
            putString(BonniApp.ATTENTIVE_DOMAIN_PREFS, domain)
        }
    AttentiveEventTracker.instance.config.changeDomain(domain)
}

fun changeEmail(viewModel: SettingsViewModel) {
    viewModel.saveEmail()
}

fun changePhoneNumber(viewModel: SettingsViewModel): Boolean {
    return viewModel.savePhoneNumber()
}

suspend fun sharePushToken(activity: Activity) {
    AttentiveSdk.getPushToken(BonniApp.getInstance(), requestPermission = false).let {
        if (it.isSuccess) {
            val token = it.getOrNull()?.token
            if (token != null) {
                // Create a share intent
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, token)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                activity.startActivity(Intent.createChooser(shareIntent, "Share Push Token"))

                Toast.makeText(BonniApp.getInstance(), "Sharing push token...", Toast.LENGTH_SHORT)
                    .show()

                Timber.d("Push token shared: $token")
            } else {
                Toast.makeText(
                    BonniApp.getInstance(),
                    "Failed to fetch push token",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } else {
            Toast.makeText(BonniApp.getInstance(), "Failed to fetch push token", Toast.LENGTH_SHORT)
                .show()
        }
    }
}

@Composable
fun SettingGroup(
    titlesToDestinations: List<Pair<String, () -> Unit>>,
    enabled: Boolean = true,
    editable: Boolean = false,
) {
    Column {
        for (titleToDestination in titlesToDestinations) {
//            if(editable){
//                EditableSetting()
//            } else {
            Setting(
                title = titleToDestination.first,
                enabled = enabled,
                onClick = titleToDestination.second,
            )
        }
        // }
    }
}

@Composable
fun Setting(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (enabled) Color.Unspecified else Color.Gray
    val onClickAction =
        if (enabled) {
            onClick
        } else {
            { Toast.makeText(BonniApp.getInstance(), "Not yet implemented", Toast.LENGTH_SHORT).show() }
        }

    Text(
        text = title,
        fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        color = textColor,
        modifier =
            Modifier
                .padding(8.dp)
                .clickable { onClickAction() },
    )
}

@Composable
fun HorizontalLine(
    color: Color = Color.Gray,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = Dp.Hairline,
        color = color,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PushPermissionRequest() {
    val context = LocalContext.current

    // Camera permission state
    val pushPermissionState =
        rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS,
        )

    if (pushPermissionState.status.isGranted) {
        Column {
            Text(
                "Push permission Granted",
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier =
                    Modifier
                        .padding(8.dp),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(
                    "Revoke push permission after app restart",
                    fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                    modifier =
                        Modifier
                            .padding(8.dp)
                            .clickable {
                                context.revokeSelfPermissionOnKill(Manifest.permission.POST_NOTIFICATIONS)
                            },
                )
            }
        }
    } else {
        Column {
            val textToShow = "Request push permission"
            Text(
                text = textToShow,
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier =
                    Modifier
                        .padding(8.dp)
                        .clickable {
                            CoroutineScope(Dispatchers.Main).launch {
                                AttentiveSdk.getPushToken(
                                    BonniApp.getInstance(),
                                    requestPermission = true,
                                )
                            }
                        },
            )
        }
    }
}

private suspend fun showOnMain(context: Context, message: String) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

private fun subscriptionResultMessage(
    result: Result<Unit>,
    successPrefix: String,
    identifier: String,
): String {
    return if (result.isSuccess) {
        "$successPrefix: $identifier"
    } else {
        "Failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
    }
}
