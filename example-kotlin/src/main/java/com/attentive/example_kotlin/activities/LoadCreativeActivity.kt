package com.attentive.example_kotlin.activities

import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import com.attentive.androidsdk.AttentiveConfig
import com.attentive.androidsdk.creatives.Creative
import com.attentive.example_kotlin.ExampleKotlinApp
import com.example.example_kotlin.R

class LoadCreativeActivity : AppCompatActivity() {
    private lateinit var creative: Creative

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_creative)
        val attentiveConfig: AttentiveConfig = (this.application as ExampleKotlinApp).attentiveConfig;

        // Attach the creative to the provided parentView
        val parentView = findViewById<View>(R.id.loadCreative).parent as View
        this.creative = Creative(attentiveConfig, parentView)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destroy the creative and it's associated WebView. You must call destroy on the
        // creative when it is no longer in use.
        creative.destroy()
    }

    fun displayCreative(view: View?) {
        // Clear cookies to avoid creative filtering during testing. Do not clear cookies
        // if you want to test Creative fatigue and filtering
        clearCookies()

        // Display the creative
        creative.trigger()
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}