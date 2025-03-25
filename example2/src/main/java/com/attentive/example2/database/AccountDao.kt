package com.attentive.example2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")fun getAll(): Flow<List<Account>>

    @Insert
    fun insert(account: Account)

    @Delete
    fun delete(account: Account)

    @Update
    fun update(account: Account)
}