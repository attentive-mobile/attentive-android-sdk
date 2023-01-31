package com.attentive.example_kotlin.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.attentive.example_kotlin.ExampleKotlinApp
import com.example.example_kotlin.R
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val domainValueTextView = findViewById<TextView>(R.id.domainValue)
        domainValueTextView.text = (this.application as ExampleKotlinApp).attentiveConfig.domain
    }

    fun startLoadCreativeActivity(view: View?) {
        val intent = Intent(this, LoadCreativeActivity::class.java)
        startActivity(intent)
    }

    fun startProductPageActivity(view: View?) {
        val intent = Intent(this, ProductPageActivity::class.java)
        startActivity(intent)
    }

    fun logoutUser(view: View?) {
        // Perform app's normal logout functionality

        // Clear all Attentive identifiers
        (this.application as ExampleKotlinApp).attentiveConfig.clearUser()
    }

    fun identifyUser(view: View?) {
        (this.application as ExampleKotlinApp).attentiveConfig.identify(ExampleKotlinApp.buildUserIdentifiers())
    }
}