package com.attentive.example_kotlin.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.attentive.example_kotlin.ExampleKotlinApp
import com.example.example_kotlin.R
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupDomainEditText()
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

    private fun setupDomainEditText() {
        val domainValueTextView = findViewById<EditText>(R.id.domainValue)
        domainValueTextView.setText((this.application as ExampleKotlinApp).attentiveConfig.domain)
        domainValueTextView.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                (this.application as ExampleKotlinApp).attentiveConfig.changeDomain(domainValueTextView.text.toString())
            }
            return@setOnEditorActionListener false
        }
    }
}