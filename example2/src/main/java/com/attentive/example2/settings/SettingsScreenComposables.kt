package com.attentive.example2.settings

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.androidsdk.creatives.Creative
import com.attentive.example2.BonniApp
import com.attentive.example2.R
import com.attentive.example2.SimpleToolbar
import com.attentive.example2.ui.theme.BonniPink
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attentive.androidsdk.internal.util.Constants
import com.attentive.example2.BonniApp.Companion.ATTENTIVE_EMAIL_PREFS
import com.attentive.example2.BonniApp.Companion.ATTENTIVE_PHONE_PREFS
import com.attentive.example2.BonniApp.Companion.ATTENTIVE_PREFS
import kotlinx.coroutines.withContext

data class SettingItem(
    val title: String,
    val enabled: Boolean = true,
    val editable: Boolean = false,
    val onClick: (String) -> Unit = {},
    val invokableComposable: @Composable ((String) -> Unit)? = null
) {
}

@Composable
fun SettingsScreen(navHostController: NavHostController) {
    SettingsScreenContent(navHostController)
}

@Composable
fun SettingsScreenContent(navHostController: NavHostController) {
    val activity = LocalActivity.current

    val frameLayout = remember {
        FrameLayout(activity!!.baseContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }


    val creative = remember {
        Creative(AttentiveEventTracker.instance.config, frameLayout, activity)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SimpleToolbar(title = "Debug Screen", {}, navHostController)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        {
            SettingsList(creative, navHostController)
            AndroidView(
                factory = { frameLayout },
                modifier = Modifier
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
fun SettingsList(creative: Creative, navHostController: NavHostController) {
    val viewModel: SettingsViewModel = viewModel()
    val accountSettings = mutableListOf<SettingItem>()
    val changeDomainSetting = SettingItem(
        title = "Change Domain",
        enabled = true,
        editable = true,
        onClick = { domain -> changeDomain(domain) }
    )

    val changeEmailSetting = SettingItem(
        title = "Change Email",
        enabled = true,
        editable = true,
        onClick = { email -> changeEmail(viewModel) }
    )

    val changePhoneNumberSetting = SettingItem(
        title = "Change Phone Number",
        enabled = true,
        editable = true,
        onClick = { phone -> changePhoneNumber(viewModel) }
    )

    val switchUserWithEmailSetting = SettingItem(
        title = "Switch User with email",
        enabled = true,
        editable = true,
        onClick = {
            viewModel.saveEmail()
            viewModel.switchUser()
        }
    )

    val switchUserWithPhoneSetting = SettingItem(
        title = "Switch User with phone",
        enabled = true,
        editable = true,
        onClick = {
            viewModel.savePhoneNumber()
            viewModel.switchUser()
        }
    )

    val apiVersionSetting = SettingItem(
        title = "Toggle Api Version",
        enabled = true,
        editable = false,
        onClick = { viewModel.toggleEndpointVersion() }
    )

    accountSettings.add(changeDomainSetting)
    accountSettings.add(changeEmailSetting)

    val debugSettings = mutableListOf<Pair<String, () -> Unit>>()
    val currentApiPreference = viewModel.getEndpointVersion()


//    debugSettings.add("Toggle Api Version - Current: $apiVersionString" to { viewModel.toggleEndpointVersion() })

    val creativeSettings = mutableListOf<Pair<String, () -> Unit>>()
    creativeSettings.add("Show Creatives" to { creative.trigger() })


    val pushSettings = mutableListOf<Pair<String, () -> Unit>>()
    pushSettings.add("Display current push token" to {
        CoroutineScope(Dispatchers.IO).launch {
            getCurrentToken()
        }
    })

    val activity = LocalActivity.current
    val context = LocalContext.current

    pushSettings.add("Share push token" to {
        CoroutineScope(Dispatchers.Main).launch {
            sharePushToken(activity!!)
        }
    })

    pushSettings.add("Update push permission status" to {
        AttentiveSdk.updatePushPermissionStatus(context)
        Toast.makeText(
            context,
            "Updating push permission status: ${AttentiveSdk.isPushPermissionGranted(context)}",
            Toast.LENGTH_SHORT
        ).show()
    })

    val deepLinkSettings = mutableListOf<Pair<String, () -> Unit>>()
    deepLinkSettings.add("Trigger Cart Deep Link Notification" to {
        triggerMockDeepLinkNotification(
            context,
            withDeepLink = true
        )
    })
    deepLinkSettings.add("Trigger No Deep Link Notification" to {
        triggerMockDeepLinkNotification(
            context,
            withDeepLink = false
        )
    })

    val userSettings = mutableListOf<Pair<String, () -> Unit>>()
    userSettings.add("Opt-In User email" to {
        CoroutineScope(Dispatchers.IO).launch {
            val email = AttentiveEventTracker.instance.config.userIdentifiers.email
            email?.let {
                AttentiveSdk.optUserIntoMarketingSubscription(email = email)
            }

            withContext(Dispatchers.Main) {
                if (email == null) {
                    Toast.makeText(context, "No email, can't opt in", Toast.LENGTH_SHORT)
                        .show()
                    return@withContext
                } else {
                    Toast.makeText(context, "Opted in user with email: $email", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    })

    userSettings.add("Opt-Out User email" to {
        CoroutineScope(Dispatchers.IO).launch {
            val email = AttentiveEventTracker.instance.config.userIdentifiers.email
            email?.let {
                AttentiveSdk.optUserOutOfMarketingSubscription(email = it)
            }
            withContext(Dispatchers.Main) {
                if (email == null) {
                    Toast.makeText(context, "No email, can't opt out", Toast.LENGTH_SHORT)
                        .show()
                    return@withContext
                } else {
                    Toast.makeText(context, "Opted out user with email: $email", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    })

    userSettings.add("Opt-In User Phone Number" to {
        CoroutineScope(Dispatchers.IO).launch {
            val phone = AttentiveEventTracker.instance.config.userIdentifiers.phone
            phone?.let {
                AttentiveSdk.optUserIntoMarketingSubscription(phoneNumber = it)
            }
            withContext(Dispatchers.Main) {
                if (phone == null) {
                    Toast.makeText(context, "No number, can't opt in", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, "Opted in user with phone: $phone", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    })

    userSettings.add("Opt-Out User Phone Number" to {
        CoroutineScope(Dispatchers.IO).launch {
            val phone = AttentiveEventTracker.instance.config.userIdentifiers.phone
            phone?.let {
                AttentiveSdk.optUserOutOfMarketingSubscription(phoneNumber = it)
            }
            withContext(Dispatchers.Main) {
                if (phone == null) {
                    Toast.makeText(context, "No number, can't opt out", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, "Opted out user with phone: $phone", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    })

    userSettings.add("Identify User" to { identifyUser() })
    userSettings.add("Clear Users" to { clearUsers(viewModel) })


    LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
        items(count = 1) {
            Text(
                "Settings",
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start,
                fontSize = 20.sp
            )
            EditableDomainSetting(changeDomainSetting)
            EditableEmailSetting(changeEmailSetting)
            EditablePhoneNumberSetting(changePhoneNumberSetting)
            SwitchUserWithEmailSetting(switchUserWithEmailSetting)
            SwitchUserWithPhoneSetting(switchUserWithPhoneSetting)
            SettingGroup(userSettings)
            ApiVersionSetting(apiVersionSetting)
            SettingGroup(creativeSettings)
            PushPermissionRequest()
            SettingGroup(pushSettings)
            SettingGroup(deepLinkSettings)
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun SwitchUserSetting(
    settingItem: SettingItem,
    value: String,
    displayLabel: String,
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    AnimatedContent(targetState = isEditing) { editing ->
        if (editing) {
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(settingItem.title) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BonniPink,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.Black,

                        ),
                    trailingIcon = {
                        IconButton(onClick = {
                            settingItem.onClick(value)
                            isEditing = false
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Submit",
                                tint = BonniPink
                            )
                        }
                    }
                )
            }
        } else {
            Text(
                text = buildAnnotatedString {
                    append("$displayLabel: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(value)
                    }
                },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { isEditing = true }
            )
        }
    }
}

@Composable
fun SwitchUserWithEmailSetting(
    settingItem: SettingItem,
    viewModel: SettingsViewModel = viewModel()
) {
    val email by viewModel.email.collectAsState()
    SwitchUserSetting(
        settingItem = settingItem,
        value = email,
        displayLabel = "Switch user with email",
        onValueChange = { viewModel.updateEmail(it) }
    )
}

@Composable
fun SwitchUserWithPhoneSetting(
    settingItem: SettingItem,
    viewModel: SettingsViewModel = viewModel()
) {
    val phone by viewModel.phone.collectAsState()
    SwitchUserSetting(
        settingItem = settingItem,
        value = phone,
        displayLabel = "Switch user with phone",
        onValueChange = { viewModel.updatePhone(it) }
    )
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
                    colors = OutlinedTextFieldDefaults.colors(
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
                                tint = BonniPink
                            )
                        }
                    }
                )
            }
        } else {
            Text(
                text = buildAnnotatedString {
                    append("Change current domain: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(domain)
                    }
                },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { isEditing = true }
            )
        }
    }
}

@Composable
fun EditableEmailSetting(settingItem: SettingItem, viewModel: SettingsViewModel = viewModel()) {
    var isEditing by remember { mutableStateOf(false) }
    val email by viewModel.email.collectAsState()

    AnimatedContent(targetState = isEditing) { editing ->
        if (editing) {
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = { Text(settingItem.title) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BonniPink,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.Black,

                        ),
                    trailingIcon = {
                        IconButton(onClick = {
                            settingItem.onClick(email)
                            isEditing = false
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Submit",
                                tint = BonniPink
                            )
                        }
                    }
                )
            }
        } else {
            Text(
                text = buildAnnotatedString {
                    append("Change current email: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(email)
                    }
                },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { isEditing = true }
            )
        }
    }
}

@Composable
fun EditablePhoneNumberSetting(
    settingItem: SettingItem,
    viewModel: SettingsViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val phone by viewModel.phone.collectAsState()
    var updatedNumber = phone

    AnimatedContent(targetState = isEditing) { editing ->
        if (editing) {
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.updatePhone(it) },
                    label = { Text(settingItem.title) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BonniPink,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.Black,

                        ),
                    trailingIcon = {
                        IconButton(onClick = {
                            settingItem.onClick(updatedNumber)
                            isEditing = false
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Submit",
                                tint = BonniPink
                            )
                        }
                    }
                )
            }
        } else {
            Text(
                text = buildAnnotatedString {
                    append("Change current phone number: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(phone)
                    }
                },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { isEditing = true }
            )
        }
    }
}

@Composable
fun EditableSwitchUserSetting(
    settingItem: SettingItem,
    viewModel: SettingsViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val email by viewModel.email.collectAsState()

    AnimatedContent(targetState = isEditing) { editing ->
        if (editing) {
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = { Text(settingItem.title) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BonniPink,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.Black,

                        ),
                    trailingIcon = {
                        IconButton(onClick = {
                            settingItem.onClick(email)
                            isEditing = false
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Submit",
                                tint = BonniPink
                            )
                        }
                    }
                )
            }
        } else {
            Text(
                text = buildAnnotatedString {
                    append("Change current email: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(email)
                    }
                },
                fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { isEditing = true }
            )
        }
    }
}

@Composable
fun ApiVersionSetting(settingItem: SettingItem, viewModel: SettingsViewModel = viewModel()) {
    val endpointVersion by viewModel.endpointVersion.collectAsState()
    Setting(
        title = endpointVersion,
        enabled = settingItem.enabled,
        onClick = { settingItem.onClick("") }
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

fun triggerMockDeepLinkNotification(context: Context, withDeepLink: Boolean) {
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
            BonniApp.getInstance()
        )
    }
}


fun identifyUser() {
    BonniApp
        .getInstance()
        .getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        .getString(ATTENTIVE_EMAIL_PREFS, "")
        ?.let {
            val identifiers = UserIdentifiers.Builder()
                .withEmail(it).build()
            AttentiveEventTracker.instance.config.identify(identifiers)
            Toast.makeText(BonniApp.getInstance(), "User identified", Toast.LENGTH_SHORT).show()
        }
}


fun clearUsers(viewModel: SettingsViewModel) {
    Timber.d("Clearing users")
    viewModel.clearPhone()
    viewModel.clearEmail()
    AttentiveEventTracker.instance.config.clearUser()
    BonniApp
        .getInstance()
        .getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        .edit {
            remove(ATTENTIVE_EMAIL_PREFS)
            remove(ATTENTIVE_PHONE_PREFS)
        }
    Toast.makeText(BonniApp.getInstance(), "Users cleared", Toast.LENGTH_SHORT).show()
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

fun changePhoneNumber(viewModel: SettingsViewModel) {
    viewModel.savePhoneNumber()
}


suspend fun sharePushToken(activity: Activity) {
    AttentiveSdk.getPushToken(BonniApp.getInstance(), requestPermission = false).let {
        if (it.isSuccess) {
            val token = it.getOrNull()?.token
            if (token != null) {
                // Create a share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
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
                    Toast.LENGTH_SHORT
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
    editable: Boolean = false
) {
    Column {
        for (titleToDestination in titlesToDestinations) {
//            if(editable){
//                EditableSetting()
//            } else {
            Setting(
                title = titleToDestination.first,
                enabled = enabled,
                onClick = titleToDestination.second
            )
        }
        // }
    }
    HorizontalLine(color = Color.Black, Modifier.padding(4.dp))
}


@Composable
fun Setting(title: String, enabled: Boolean, onClick: () -> Unit) {
    val textColor = if (enabled) Color.Unspecified else Color.Gray
    val onClickAction = if (enabled) onClick else {
        { Toast.makeText(BonniApp.getInstance(), "Not yet implemented", Toast.LENGTH_SHORT).show() }
    }

    Text(
        text = title,
        fontFamily = FontFamily(Font(R.font.degulardisplay_regular)),
        color = textColor,
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClickAction() }
    )
}

@Composable
fun HorizontalLine(
    color: Color = Color.Gray,
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
private fun PushPermissionRequest() {
    val context = LocalContext.current

    // Camera permission state
    val pushPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS
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
                            AttentiveSdk.getPushToken(
                                BonniApp.getInstance(),
                                requestPermission = true
                            )
                        }
                    }
            )
        }
    }
}