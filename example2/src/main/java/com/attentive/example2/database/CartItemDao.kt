package com.attentive.example2.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CartItemDao {
    @Query("SELECT * FROM cart_items")
    fun getAll(): List<CartItem>

    @Insert
    fun insert(cartItem: CartItem)

    @Delete
    fun delete(cartItem: CartItem)

    @Update
    fun update(cartItem: CartItem)
}