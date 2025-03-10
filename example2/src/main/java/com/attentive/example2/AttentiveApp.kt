package com.attentive.example2

import android.app.Application
import androidx.room.Room
import com.attentive.example2.database.AppDatabase

class AttentiveApp: Application() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this
    }

    companion object{
        private lateinit var appInstance: AttentiveApp
        fun getInstance(): AttentiveApp{
            return appInstance
        }
    }

}