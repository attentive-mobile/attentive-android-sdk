package com.attentive.example2.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.attentive.androidsdk.events.Item

@Entity(tableName = "product_items")
data class ExampleProduct(@PrimaryKey val id: String, @TypeConverters(Converters::class) val item: Item, val imageId: Int)
