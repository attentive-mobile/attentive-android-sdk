package com.attentive.example2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CartItemDao {
    @Query("SELECT * FROM cartitem")
    fun getAll(): List<CartItem> {
        return listOf()
    }

    @Insert
    fun insert(cartItem: CartItem)

    @Delete
    fun delete(cartItem: CartItem)
}