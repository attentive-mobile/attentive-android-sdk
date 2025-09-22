package com.attentive.example2.settings

import android.content.Context.MODE_PRIVATE
import android.provider.Settings.Global.putString
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.example2.BonniApp
import com.attentive.example2.BonniApp.Companion.ATTENTIVE_PHONE_PREFS
import com.attentive.example2.BonniApp.Companion.ATTENTIVE_PREFS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit
import com.attentive.androidsdk.AttentiveSdk
import com.attentive.example2.BonniApp.Companion.ATTENTIVE_EMAIL_PREFS

class SettingsViewModel : ViewModel() {
    private val _phone = MutableStateFlow(
        AttentiveEventTracker.instance.config.userIdentifiers.phone
            ?.takeIf { it.isNotEmpty() }
            ?: getPersistedPhoneNumber()
    )
    val phone: StateFlow<String> = _phone as StateFlow<String>

    private val _email = MutableStateFlow(
        if (!AttentiveEventTracker.instance.config.userIdentifiers.email.isNullOrEmpty())
            AttentiveEventTracker.instance.config.userIdentifiers.email
        else
            getPersistedEmail()
    )
    val email: StateFlow<String> = _email as StateFlow<String>

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
                    phone.value
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
                email.value
            )
        }

        val identifiers = UserIdentifiers.Builder().withEmail(email.value).build()
        AttentiveEventTracker.instance.config.identify(identifiers)
    }

    fun getPersistedEmail(): String {
        return BonniApp.getInstance().getSharedPreferences(ATTENTIVE_PREFS, MODE_PRIVATE)
            .getString(ATTENTIVE_EMAIL_PREFS, "") ?: ""
    }

    fun loginUser(){
        AttentiveSdk.updateUser(getPersistedEmail(), getPersistedPhoneNumber())
        Toast.makeText(BonniApp.getInstance(), "Login with email: ${getPersistedEmail()} and phone ${getPersistedPhoneNumber()}", Toast.LENGTH_SHORT).show()
    }
}