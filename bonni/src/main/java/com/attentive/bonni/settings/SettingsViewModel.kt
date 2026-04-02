package com.attentive.bonni.settings

import android.content.Context.MODE_PRIVATE
import android.provider.Settings.Global.putString
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.androidsdk.internal.network.ApiVersion
import com.attentive.bonni.BonniApp
import com.attentive.bonni.BonniApp.Companion.ATTENTIVE_EMAIL_PREFS
import com.attentive.bonni.BonniApp.Companion.ATTENTIVE_ENDPOINT_PREFS
import com.attentive.bonni.BonniApp.Companion.ATTENTIVE_PHONE_PREFS
import com.attentive.bonni.BonniApp.Companion.ATTENTIVE_PREFS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {
    private val _phone =
        MutableStateFlow(
            AttentiveEventTracker.instance.config.userIdentifiers.phone
                ?.takeIf { it.isNotEmpty() }
                ?: getPersistedPhoneNumber(),
        )
    val phone: StateFlow<String> = _phone as StateFlow<String>

    private val _email =
        MutableStateFlow(
            if (!AttentiveEventTracker.instance.config.userIdentifiers.email.isNullOrEmpty()) {
                AttentiveEventTracker.instance.config.userIdentifiers.email
            } else {
                getPersistedEmail()
            },
        )
    val email: StateFlow<String> = _email as StateFlow<String>

    private val _endpointVersion =
        if (getEndpointVersion() == ApiVersion.OLD) {
            MutableStateFlow("Toggle Api Version - Current: Old Endpoint")
        } else {
            MutableStateFlow("Toggle Api Version - Current: New Endpoint")
        }
    val endpointVersion: StateFlow<String> = _endpointVersion

    fun updatePhone(newPhone: String) {
        _phone.value = newPhone
    }

    fun clearPhone() {
        _phone.value = ""
    }

    fun savePhoneNumber() {
        BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE).edit {
            putString(
                ATTENTIVE_PHONE_PREFS,
                phone.value,
            )
        }

        val identifiers = UserIdentifiers.Builder().withPhone(phone.value).build()
        AttentiveEventTracker.instance.config.identify(identifiers)
    }

    fun getPersistedPhoneNumber(): String {
        return BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
            .getString(ATTENTIVE_PHONE_PREFS, "") ?: ""
    }

    fun updateEmail(newEmail: String) {
        _email.value = newEmail
    }

    fun clearEmail() {
        _email.value = ""
    }

    fun saveEmail() {
        BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE).edit {
            putString(
                ATTENTIVE_EMAIL_PREFS,
                email.value,
            )
        }

        val identifiers = UserIdentifiers.Builder().withEmail(email.value).build()
        AttentiveEventTracker.instance.config.identify(identifiers)
    }

    fun getPersistedEmail(): String {
        return BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
            .getString(ATTENTIVE_EMAIL_PREFS, "") ?: ""
    }

    fun getEndpointVersion(): ApiVersion {
        val prefs = BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        return try {
            ApiVersion.valueOf(prefs.getString(ATTENTIVE_ENDPOINT_PREFS, null) ?: "OLD")
        } catch (e: IllegalArgumentException) {
            ApiVersion.OLD
        }
    }

    fun toggleEndpointVersion() {
        val prefs = BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
        val currentApiVersion = getEndpointVersion()

        val newApiVersion =
            if (currentApiVersion == ApiVersion.OLD) {
                ApiVersion.NEW
            } else {
                ApiVersion.OLD
            }

        // Save to SharedPrefs for persistence across app restarts
        prefs.edit {
            putString(ATTENTIVE_ENDPOINT_PREFS, newApiVersion.name)
        }

        // Update the runtime config so it takes effect immediately
        AttentiveEventTracker.instance.config.changeApiVersion(newApiVersion)

        val endPointString =
            if (newApiVersion == ApiVersion.OLD) {
                "Old Endpoint"
            } else {
                "New Endpoint"
            }

        _endpointVersion.value = "Toggle Api Version - Current: $endPointString"
    }

    fun switchUser() {
        AttentiveSdk.updateUser(getPersistedEmail(), getPersistedPhoneNumber())
        Toast.makeText(
            BonniApp.getInstance(),
            "Switch to user with email: ${getPersistedEmail()} and phone ${getPersistedPhoneNumber()}",
            Toast.LENGTH_SHORT,
        ).show()
    }
}
