package com.attentive.example2.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.attentive.androidsdk.events.Item


@Entity(tableName = "cart_items")
data class ExampleCartItem(@PrimaryKey val id: String, @TypeConverters(Converters::class) val product: ExampleProduct, val quantity: Int)
