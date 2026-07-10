package com.attentive.androidsdk.internal.network.buffer

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Database(entities = [BufferedRequestEntity::class], version = 2, exportSchema = false)
abstract class BufferDatabase : RoomDatabase() {
    abstract fun bufferedRequestDao(): BufferedRequestDao

    companion object {
        private const val DATABASE_NAME = "attentive_buffer.db"

        @Volatile
        private var instance: BufferDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): BufferDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BufferDatabase::class.java,
                    DATABASE_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
