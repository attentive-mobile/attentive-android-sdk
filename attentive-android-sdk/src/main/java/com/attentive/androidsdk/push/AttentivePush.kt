package com.attentive.androidsdk.push

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.attentive.androidsdk.AttentiveApi
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import timber.log.Timber

class AttentivePush {

    fun fetchPushToken(context: Context, callback: Result<String>) {
        if(!checkPushPermission(context)){
            requestPushPermission(context, callback)
        }
    }

    fun checkPushPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPushPermission(context: Context, callback: Result<String>) {
        PermissionRequestActivity.request(context, callback)
    }

    private fun getTokenFromFirebase(callback: Result<String>){
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            Timber.d("Token: $token")
            callback.onSuccess({ token })
        }
    }

    companion object{
        lateinit var INSTANCE: AttentivePush
        fun getInstance(): AttentivePush {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = AttentivePush()
            }
            return INSTANCE
        }
    }

    class PermissionRequestActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            Timber.d("PermissionRequestActivity onCreate")
            super.onCreate(savedInstanceState)
            activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        private val activityResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                Timber.d("PermissionRequestActivity isGranted: $isGranted")
                // Handle Permission granted/rejected
                if (isGranted) {
                    // Permission is granted
                    Timber.d("Push permission granted")
                    AttentivePush.getInstance().getTokenFromFirebase(callback!!)
                } else {
                    Timber.d("Push permission denied")
                    callback?.onFailure { "" }
                }

                finish()
            }

        companion object {
            private var callback: Result<String>? = null
            fun request(context: Context, callback: Result<String>) {
                Timber.d("PermissionRequestActivity request")
                val intent = Intent(context, PermissionRequestActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}