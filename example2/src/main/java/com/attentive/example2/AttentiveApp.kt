package com.attentive.example2

import android.app.Application
import androidx.room.Room
import com.attentive.example2.database.AppDatabase

class AttentiveApp: Application() {
    private lateinit var _database: AppDatabase

    override fun onCreate() {
        super.onCreate()
    }

    fun getDatabase(): AppDatabase {
        if(!::_database.isInitialized) {
            _database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "database-name"
            ).build()
        }

        return _database
    }
}