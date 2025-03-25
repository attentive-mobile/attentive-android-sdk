package com.attentive.example2.welcome

import android.provider.ContactsContract.Data
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Database
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.example2.AttentiveApp
import com.attentive.example2.database.Account
import com.attentive.example2.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpViewModel: ViewModel() {


    fun onSubmit(email: String, password: String){
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().accountDao().insert(Account(email, password, signedIn = true))
            val config = AttentiveConfig.Builder().build()
            val identifiers = UserIdentifiers.Builder().withEmail(email).build()
            config.identify(identifiers)

            val attentiveConfig =
                AttentiveConfig
                    .Builder()
                    .context(AttentiveApp.getInstance().applicationContext)
                    .domain("games")
                    .mode(AttentiveConfig.Mode.DEBUG)
                    .logLevel(AttentiveLogLevel.VERBOSE).build()

            AttentiveEventTracker.instance.initialize(attentiveConfig)

        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}