package com.anurag.aiprompto.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.anurag.aiprompto.model.BaseResponse
import com.anurag.aiprompto.model.TokenRequest
import com.anurag.aiprompto.BuildConfig

object TokenManager {
    private const val PREFS_NAME = "fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_TOKEN_SENT = "token_sent"

    fun sendTokenToServer(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldToken = prefs.getString(KEY_FCM_TOKEN, "")

        // Only send if token changed or wasn't sent before
        if (oldToken != token || !prefs.getBoolean(KEY_TOKEN_SENT, false)) {
            sendTokenToBackend(context, token, prefs)
        }
    }

    private fun sendTokenToBackend(context: Context, token: String, prefs: SharedPreferences) {
        // Use your existing ApiClient instance
        val apiService = ApiClient.getInstance().getApiService()

        // Create token request - you'll need to define this data class
        val request = TokenRequest(
            token = token,
            deviceId = getDeviceId(context),
            appVersion = BuildConfig.VERSION_NAME,
            osVersion = Build.VERSION.RELEASE
        )

        // You'll need to add this endpoint to your existing ApiService
        apiService.registerFCMToken(request).enqueue(object : retrofit2.Callback<BaseResponse> {
            override fun onResponse(call: retrofit2.Call<BaseResponse>, response: retrofit2.Response<BaseResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    // Mark token as sent successfully
                    prefs.edit()
                        .putString(KEY_FCM_TOKEN, token)
                        .putBoolean(KEY_TOKEN_SENT, true)
                        .apply()
                    Log.d("TokenManager", "✅ Token sent to server successfully")
                } else {
                    Log.e("TokenManager", "❌ Failed to send token: ${response.code()}")
                    // Retry logic can be added here
                }
            }

            override fun onFailure(call: retrofit2.Call<BaseResponse>, t: Throwable) {
                Log.e("TokenManager", "❌ Error sending token to server", t)
                // Retry logic can be added here
            }
        })
    }

    private fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            "unknown_device"
        }
    }

    fun getStoredToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun isTokenSent(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TOKEN_SENT, false)
    }
}