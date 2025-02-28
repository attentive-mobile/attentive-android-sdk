package com.attentive.example2

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.attentive.androidsdk.AttentiveEventTracker
import com.attentive.androidsdk.events.Item
import com.attentive.androidsdk.events.ProductViewEvent

class ProductViewModel : ViewModel() {
    private val items = mutableListOf<Item>()

    fun addToCart() {

    }

    fun productWasViewed(item: Item) {
        if(items.contains(item).not()) {
            items.add(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.e("pfaff", "onCleared")
        val event = ProductViewEvent.Builder().items(items).build()
        AttentiveEventTracker.instance.recordEvent(event)
    }
}