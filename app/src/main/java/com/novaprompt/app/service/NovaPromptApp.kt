package com.novaprompt.app.service

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.novaprompt.app.service.TokenManager

class NovaPromptApp : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this) {}

        initializeFCM()
    }

    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "🚀 FCM Token obtained: ${token.take(20)}...")

                TokenManager.sendTokenToServer(this, token)
            } else {
                Log.e("FCM", "❌ Failed to get FCM token", task.exception)
            }
        }

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