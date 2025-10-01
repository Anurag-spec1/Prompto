package com.novaprompt.app

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.novaprompt.app.service.TokenManager

class NovaPromptApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this) {}

        // Initialize FCM
        initializeFCM()
    }

    private fun initializeFCM() {
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "🚀 FCM Token obtained: ${token.take(20)}...")

                // Send token to your server using existing ApiClient structure
                TokenManager.sendTokenToServer(this, token)
            } else {
                Log.e("FCM", "❌ Failed to get FCM token", task.exception)
            }
        }

        // Subscribe to topic for broadcast notifications
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "✅ Subscribed to 'all_users' topic")
                } else {
                    Log.e("FCM", "❌ Failed to subscribe to topic", task.exception)
                }
            }
    }
}