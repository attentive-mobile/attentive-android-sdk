package com.attentive.bonni.database

import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.Price
import com.attentive.bonni.BonniApp
import com.attentive.bonni.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

@Database(entities = [ExampleCartItem::class, ExampleProduct::class, Account::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartItemDao(): ExampleCartItemDao
    abstract fun productItemDao(): ExampleProductDao
    abstract fun accountDao(): AccountDao

    internal fun initWithMockProducts() {
        Timber.d("initWithMockProducts")
        CoroutineScope(Dispatchers.IO).launch {
            val count = productItemDao().getAll().first().count()
            if( count > 0) {
                Timber.d("${count}Products already exist, skipping initialization")
                return@launch
            }
            val itemCount = 4
            val imageIds =
                listOf(
                    R.drawable.superscreen,
                    R.drawable.stick1,
                    R.drawable.balm2,
                    R.drawable.balm3
                )
            val names = listOf("Protective Sunscreen", "The Stick", "The Balm", "The Balm")
            val prices = listOf(
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(12)).build(),
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(20)).build(),
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(15)).build(),
                Price.Builder().currency(Currency.getInstance(Locale.getDefault()))
                    .price(BigDecimal(13)).build()
            )
            for (i in 0..(itemCount - 1)) {
                val item =
                    Item.Builder("productId$i", "variantId$i", prices[i]).name(names[i]).build()
                val product = ExampleProduct("$i", item, imageIds[i])
                Timber.d("initWithMockProducts: $product")
                productItemDao().insert(product)
            }
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    BonniApp.getInstance().applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

