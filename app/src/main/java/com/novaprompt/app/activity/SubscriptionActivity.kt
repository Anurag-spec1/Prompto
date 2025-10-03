package com.novaprompt.app.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.novaprompt.app.R
import com.novaprompt.app.databinding.ActivitySubscriptionBinding


class SubscriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var billingClient: BillingClient
    private var purchasedSkus = mutableListOf<Purchase>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        setupClickListeners()
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                } else {
                    Log.e("Billing", "Billing setup failed: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d("Billing", "Billing service disconnected")
            }
        })
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("Billing", "User cancelled purchase")
            Toast.makeText(this, "Purchase cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("Billing", "Purchase failed: ${billingResult.responseCode}")
            Toast.makeText(this, "Purchase failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            finish()
        }

        findViewById<carbon.widget.Button>(R.id.btnSubscribe).setOnClickListener {
            Log.d("Subscription", "Subscribe button clicked")
            launchSubscriptionFlow()
        }

        findViewById<TextView>(R.id.tvTerms).setOnClickListener {
            startActivity(Intent(this, TermsAndConditions::class.java))
        }

        findViewById<TextView>(R.id.tvPrivacy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicy::class.java))
        }

        findViewById<TextView>(R.id.tvRestore).setOnClickListener {
            queryPurchases()
            Toast.makeText(this, "Checking for existing purchases...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchSubscriptionFlow() {
        Log.d("Subscription", "Launching subscription flow")
        val productId = "purchase_149"
        queryProductDetails(productId)
    }

    private fun queryProductDetails(productId: String) {
        Log.d("Subscription", "Querying product details for: $productId")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        if (!billingClient.isReady) {
            Log.e("Subscription", "Billing client not ready")
            Toast.makeText(this, "Billing service not ready. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            Log.d("Subscription", "Product details query result: ${billingResult.responseCode}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                if (productDetailsList.productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList.productDetailsList[0]
                    Log.d("Subscription", "Product details found: ${productDetails.name}")
                    launchBillingFlow(productDetails)
                } else {
                    Log.e("Subscription", "No product details found")
                    runOnUiThread {
                        Toast.makeText(this, "Subscription product not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.e("Subscription", "Query product details failed: ${billingResult.responseCode}")
                runOnUiThread {
                    Toast.makeText(this, "Failed to load subscription details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchBillingFlow(productDetails: ProductDetails) {
        Log.d("Subscription", "Launching billing flow")

        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
        if (subscriptionOfferDetails.isNullOrEmpty()) {
            Log.e("Subscription", "No subscription offers found")
            Toast.makeText(this, "No subscription offers available", Toast.LENGTH_SHORT).show()
            return
        }

        val offerToken = subscriptionOfferDetails[0].offerToken

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val billingResult = billingClient.launchBillingFlow(this, billingFlowParams)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e("Subscription", "Launch billing flow failed: ${billingResult.responseCode}")
            Toast.makeText(this, "Failed to start purchase flow", Toast.LENGTH_SHORT).show()
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            Log.e("Subscription", "Billing client not ready for query")
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            Log.d("Subscription", "Query purchases result: ${billingResult.responseCode}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasedSkus.clear()
                purchasedSkus.addAll(purchases)
                updateUIForSubscriptionStatus()

                if (purchases.isNotEmpty()) {
                    Log.d("Subscription", "Found ${purchases.size} purchases")
                }
            } else {
                Log.e("Subscription", "Query purchases failed: ${billingResult.responseCode}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d("Subscription", "Handling purchase: ${purchase.products}")

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("Subscription", "Purchase acknowledged successfully")
                        purchasedSkus.add(purchase)
                        updateUIForSubscriptionStatus()
                        showSuccessMessage()
                    } else {
                        Log.e("Subscription", "Purchase acknowledgement failed: ${billingResult.responseCode}")
                    }
                }
            } else {
                Log.d("Subscription", "Purchase already acknowledged")
                purchasedSkus.add(purchase)
                updateUIForSubscriptionStatus()
            }
        }
    }

    private fun updateUIForSubscriptionStatus() {
        runOnUiThread {
            val isSubscribed = isUserSubscribed()
            val subscribeButton = findViewById<carbon.widget.Button>(R.id.btnSubscribe)

            if (isSubscribed) {
                subscribeButton.text = "Already Subscribed"
                subscribeButton.isEnabled = false
                subscribeButton.alpha = 0.7f
                Toast.makeText(this, "You're already a premium user!", Toast.LENGTH_SHORT).show()
            } else {
                subscribeButton.text = "Buy Subscription"
                subscribeButton.isEnabled = true
                subscribeButton.alpha = 1f
            }
        }
    }

    private fun isUserSubscribed(): Boolean {
        return purchasedSkus.any { purchase ->
            purchase.products.any { productId ->
                productId == "premium_subscription_monthly" // Your subscription product ID
            } && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    private fun showSuccessMessage() {
        runOnUiThread {
            Toast.makeText(this, "Welcome to Premium! 🎉", Toast.LENGTH_LONG).show()

        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::billingClient.isInitialized && billingClient.isReady) {
            queryPurchases()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}