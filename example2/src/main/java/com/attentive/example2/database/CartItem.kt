package com.attentive.example2.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Price


@Entity(tableName = "cart_items")
data class CartItem(@PrimaryKey val id: String, @TypeConverters(Converters::class) val item: Item, val quantity: Int)
