package com.attentive.bonni.database

import androidx.room.TypeConverter
import com.attentive.androidsdk.events.Item
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class Converters {
    @TypeConverter
    fun fromItem(item: Item): String {
        return Gson().toJson(item)
    }

    @TypeConverter
    fun toItem(itemString: String): Item {
        val itemType = object : TypeToken<Item>() {}.type
        return Gson().fromJson(itemString, itemType)
    }

    @TypeConverter
    fun fromExampleProduct(exampleProduct: ExampleProduct): String {
        return Gson().toJson(exampleProduct)
    }

    @TypeConverter
    fun toExampleProduct(exampleProductString: String): ExampleProduct {
        val type = object : TypeToken<ExampleProduct>() {}.type
        return Gson().fromJson(exampleProductString, type)
    }
}