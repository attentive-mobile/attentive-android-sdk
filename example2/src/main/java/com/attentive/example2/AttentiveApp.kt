package com.attentive.example2

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.AttentiveLogLevel
import com.attentive.example2.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttentiveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        initAttentiveTracker()
    }

    private fun initAttentiveTracker() {
        val attentiveConfig =
            AttentiveConfig
                .Builder()
                .context(this)
                .domain("YOUR_ATTENTIVE_DOMAIN")
                .mode(AttentiveConfig.Mode.DEBUG)
                .logLevel(AttentiveLogLevel.VERBOSE).build()

        AttentiveEventTracker.instance.initialize(attentiveConfig)
    }

    companion object {
        private lateinit var appInstance: AttentiveApp
        fun getInstance(): AttentiveApp {
            return appInstance
        }
    }

}