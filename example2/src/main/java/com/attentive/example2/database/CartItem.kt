package com.attentive.example2.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class CartItem(
    @PrimaryKey val name: String, val price: Double, val quantity: Int
)