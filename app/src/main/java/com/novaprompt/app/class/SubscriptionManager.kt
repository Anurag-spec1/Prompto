package com.novaprompt.app.`class`

import android.content.Context
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

    interface SubscriptionListener {
        fun onSubscriptionStatusChanged(isSubscribed: Boolean)
    }

    fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
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
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        })
    }

    fun checkSubscriptionStatus(callback: (Boolean) -> Unit) {
        if (billingClient == null || !billingClient!!.isReady) {
            callback(false)
            return
        }

        queryPurchases { purchases ->
            val isSubscribed = purchases.any { purchase ->
                purchase.products.any { it.startsWith("premium_") } &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            callback(isSubscribed)
        }
    }

    private fun queryPurchases(callback: (List<Purchase>) -> Unit = {}) {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasedSkus.clear()
                purchasedSkus.addAll(purchases)
                callback(purchases)

                val isSubscribed = isUserSubscribed()
                listeners.forEach { it.onSubscriptionStatusChanged(isSubscribed) }
            } else {
                callback(emptyList())
            }
        }
    }

    fun isUserSubscribed(): Boolean {
        return purchasedSkus.any { purchase ->
            purchase.products.any { productId ->
                productId.startsWith("premium_")
            } && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    fun addSubscriptionListener(listener: SubscriptionListener) {
        listeners.add(listener)
    }

    fun removeSubscriptionListener(listener: SubscriptionListener) {
        listeners.remove(listener)
    }
}