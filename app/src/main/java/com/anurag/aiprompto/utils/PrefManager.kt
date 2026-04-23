package com.anurag.aiprompto.utils

import android.content.Context

class PrefManager(context: Context) {
    private val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    fun getOpenCount(): Int {
        return prefs.getInt("app_open_count", 0)
    }

    fun incrementOpenCount() {
        val newCount = getOpenCount() + 1
        prefs.edit().putInt("app_open_count", newCount).apply()
    }
}
