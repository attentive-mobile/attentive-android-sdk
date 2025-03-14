package com.attentive.example2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Price
import com.attentive.example2.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

@Database(entities = [ExampleCartItem::class, ExampleProduct::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartItemDao(): ExampleCartItemDao
    abstract fun productItemDao(): ExampleProductDao

    private fun initWithMockProducts() {
        CoroutineScope(Dispatchers.IO).launch {
            val imageIds =
                listOf(R.drawable.tshirt, R.drawable.cat_tree, R.drawable.coffee, R.drawable.vinyl)
            val names = listOf("T-Shirt", "Cat Tree", "Coffee", "Vinyl")
            val prices = listOf(
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(20.0)).build(),
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(100.0)).build(),
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(15.0)).build(),
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(37.50)).build()
            )
            for (i in 0..3) {
                val item =
                    Item.Builder("productId$i", "variantId$i", prices[i]).name(names[i]).build()
                val product = ExampleProduct("$i", item, imageIds[i])
                productItemDao().insert(product)
            }
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                CoroutineScope(Dispatchers.IO).launch {
                    instance.initWithMockProducts()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}

