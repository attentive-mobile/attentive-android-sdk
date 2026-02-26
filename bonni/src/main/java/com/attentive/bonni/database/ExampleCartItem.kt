package com.attentive.bonni.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "cart_items")
data class ExampleCartItem(
    @PrimaryKey val id: String,
    @TypeConverters(Converters::class) val product: ExampleProduct,
    val quantity: Int,
)
