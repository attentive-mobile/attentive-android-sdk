package com.attentive.example2

import android.app.Application
import androidx.room.Room
import com.attentive.example2.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttentiveApp: Application() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance().clearAllTables()
        }
    }

    companion object{
        private lateinit var appInstance: AttentiveApp
        fun getInstance(): AttentiveApp{
            return appInstance
        }
    }

}