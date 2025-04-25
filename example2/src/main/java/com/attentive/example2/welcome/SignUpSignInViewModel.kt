package com.attentive.example2.welcome

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.androidsdk.UserIdentifiers
import com.attentive.example2.AttentiveApp
import com.attentive.example2.database.Account
import com.attentive.example2.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SignUpSignInViewModel(application: Application): AndroidViewModel(AttentiveApp.getInstance()) {


    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    fun onSignUp(email: String, password: String){
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().accountDao().insert(Account(email, password, signedIn = true))
           identifyUser(email)
            _signedIn.value = true
        }
    }

    fun onSignIn(email: String, password: String){
        var accountFound = false
        viewModelScope.launch(Dispatchers.IO){
            AppDatabase.getInstance().accountDao().getAll().collect{
                it.forEach { account ->
                    if(account.email == email && account.password == password){
                        accountFound = true
                        identifyUser(email)
                        _signedIn.value = true
                    }
                }
            }
        }

        if(!accountFound){
            Toast.makeText(getApplication(), "Account not found", Toast.LENGTH_SHORT).show()   // Show error message
        }
    }

    private fun identifyUser(email: String){
        val identifiers = UserIdentifiers.Builder().withEmail(email).build()
        AttentiveEventTracker.instance.config?.identify(identifiers)
    }

    override fun onCleared() {
        super.onCleared()
    }
}