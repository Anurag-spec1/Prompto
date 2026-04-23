package com.anurag.aiprompto.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SubscriptionManager {

    private const val PREF_NAME = "subscription_prefs"
    private const val KEY_SUBSCRIBED = "is_subscribed"
    private const val KEY_PAYMENT_ID = "payment_id"
    private const val KEY_PURCHASE_TIME = "purchase_time"

    private lateinit var prefs: SharedPreferences
    private var isInitialized = false
    private val listeners = mutableListOf<SubscriptionListener>()

    interface SubscriptionListener {
        fun onSubscriptionStatusChanged(isSubscribed: Boolean)
    }

    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            isInitialized = true
            Log.d("SubscriptionManager", "Initialized with subscription = ${isUserSubscribed()}")
        }
    }

    /**
     * Call this after a successful Razorpay payment.
     */
    fun setUserSubscribed(subscribed: Boolean, paymentId: String? = null) {
        if (!isInitialized) {
            Log.e("SubscriptionManager", "Not initialized. Call initialize() first.")
            return
        }
        with(prefs.edit()) {
            putBoolean(KEY_SUBSCRIBED, subscribed)
            if (paymentId != null) {
                putString(KEY_PAYMENT_ID, paymentId)
                putLong(KEY_PURCHASE_TIME, System.currentTimeMillis())
            }
            apply()
        }
        Log.d("SubscriptionManager", "Subscription set to $subscribed")
        notifyListeners()
    }

    fun isUserSubscribed(): Boolean {
        if (!isInitialized) return false
        return prefs.getBoolean(KEY_SUBSCRIBED, false)
    }

    fun checkSubscriptionStatus(callback: (Boolean) -> Unit) {
        callback(isUserSubscribed())
    }

    fun getPaymentId(): String? {
        return if (isInitialized) prefs.getString(KEY_PAYMENT_ID, null) else null
    }

    fun getPurchaseTime(): Long {
        return if (isInitialized) prefs.getLong(KEY_PURCHASE_TIME, 0L) else 0L
    }

    fun addSubscriptionListener(listener: SubscriptionListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            listener.onSubscriptionStatusChanged(isUserSubscribed())
        }
    }

    fun removeSubscriptionListener(listener: SubscriptionListener) {
        listeners.remove(listener)
    }

    fun refreshSubscriptionStatus() {
        notifyListeners()
    }

    private fun notifyListeners() {
        val status = isUserSubscribed()
        listeners.forEach {
            it.onSubscriptionStatusChanged(status)
        }
    }

    /**
     * Optional: clear subscription (e.g., for testing or expiry)
     */
    fun clearSubscription() {
        if (!isInitialized) return
        with(prefs.edit()) {
            clear()
            apply()
        }
        Log.d("SubscriptionManager", "Subscription cleared")
        notifyListeners()
    }
}