package com.attentive.example2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExampleCartItemDao {
    @Query("SELECT * FROM cart_items")
    fun getAll(): Flow<List<ExampleCartItem>>

    @Insert
    fun insert(exampleCartItem: ExampleCartItem)

    @Delete
    fun delete(exampleCartItem: ExampleCartItem)

    @Update
    fun update(exampleCartItem: ExampleCartItem)
}