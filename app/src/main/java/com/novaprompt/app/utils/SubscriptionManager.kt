package com.novaprompt.app.utils

import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams

object SubscriptionManager {

    private var billingClient: BillingClient? = null
    private var purchasedSkus = mutableListOf<Purchase>()
    private var listeners = mutableListOf<SubscriptionListener>()
    private var isInitialized = false

    interface SubscriptionListener {
        fun onSubscriptionStatusChanged(isSubscribed: Boolean)
    }

    fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                purchases?.let {
                    processPurchases(it)
                    it.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isInitialized = true
                    queryPurchases()
                    Log.d("SubscriptionManager", "Billing client initialized successfully")
                } else {
                    isInitialized = false
                    Log.e("SubscriptionManager", "Billing setup failed: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isInitialized = false
                Log.d("SubscriptionManager", "Billing service disconnected")
            }
        })
    }

    fun checkSubscriptionStatus(callback: (Boolean) -> Unit) {
        if (!isInitialized || billingClient == null || !billingClient!!.isReady) {
            Log.d("SubscriptionManager", "Billing client not ready, returning false")
            callback(false)
            return
        }

        queryPurchases { purchases ->
            val isSubscribed = purchases.any { purchase ->
                purchase.products.any { productId ->
                    productId == "purchase_149" ||
                            productId.startsWith("premium_") ||
                            productId.contains("subscription")
                } && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            Log.d("SubscriptionManager", "Subscription status checked: $isSubscribed")
            callback(isSubscribed)
        }
    }

    private fun queryPurchases(callback: (List<Purchase>) -> Unit = {}) {
        if (!isInitialized) {
            callback(emptyList())
            return
        }

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d("SubscriptionManager", "Found ${purchases.size} subscriptions")
                purchasedSkus.clear()
                purchasedSkus.addAll(purchases)
                callback(purchases)

                val isSubscribed = isUserSubscribed()
                Log.d("SubscriptionManager", "Notifying listeners: $isSubscribed")
                listeners.forEach {
                    it.onSubscriptionStatusChanged(isSubscribed)
                }
            } else {
                Log.e("SubscriptionManager", "Query purchases failed: ${billingResult.responseCode}")
                callback(emptyList())
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        Log.d("SubscriptionManager", "Processing ${purchases.size} purchases")
        purchasedSkus.clear()
        purchasedSkus.addAll(purchases)
        val isSubscribed = isUserSubscribed()
        Log.d("SubscriptionManager", "Purchase processed, subscribed: $isSubscribed")
        listeners.forEach {
            it.onSubscriptionStatusChanged(isSubscribed)
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d("SubscriptionManager", "Purchase acknowledged successfully")
            } else {
                Log.e("SubscriptionManager", "Purchase acknowledgement failed: ${billingResult.responseCode}")
            }
        }
    }

    fun isUserSubscribed(): Boolean {
        val isSubscribed = purchasedSkus.any { purchase ->
            purchase.products.any { productId ->
                productId == "purchase_149" ||
                        productId.startsWith("premium_") ||
                        productId.contains("subscription")
            } && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        Log.d("SubscriptionManager", "isUserSubscribed: $isSubscribed")
        return isSubscribed
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
        if (isInitialized) {
            queryPurchases()
        }
    }
}