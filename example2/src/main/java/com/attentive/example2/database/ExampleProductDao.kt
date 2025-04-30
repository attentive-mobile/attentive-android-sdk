package com.attentive.example2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExampleProductDao {
    @Query("SELECT * FROM product_items")fun getAll(): Flow<List<ExampleProduct>>

    @Insert
    fun insert(exampleProduct: ExampleProduct)

    @Delete
    fun delete(exampleProduct: ExampleProduct)

    @Update
    fun update(exampleProduct: ExampleProduct)
}