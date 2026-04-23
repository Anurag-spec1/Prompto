package com.anurag.aiprompto.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anurag.aiprompto.R
import com.anurag.aiprompto.utils.SubscriptionManager
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class SubscriptionActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var checkout: Checkout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        // Initialize Razorpay
        checkout = Checkout()
        checkout.setKeyID("rzp_test_SAqejDSieFQMca")   // Replace with your actual key

        setupClickListeners()
        updateUIForSubscriptionStatus()
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            finish()
        }

        findViewById<carbon.widget.Button>(R.id.btnSubscribe).setOnClickListener {
            startPayment()
        }

        findViewById<TextView>(R.id.tvTerms).setOnClickListener {
            startActivity(Intent(this, TermsAndConditions::class.java))
        }

        findViewById<TextView>(R.id.tvPrivacy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicy::class.java))
        }

        findViewById<TextView>(R.id.tvRestore).setOnClickListener {
            checkExistingSubscription()
        }
    }

    private fun startPayment() {
        val amount = 14900   // ₹149 in paise

        val options = JSONObject().apply {
            put("name", "AI Prompto Premium")
            put("description", "Unlock all prompts and generate images")
            put("currency", "INR")
            put("amount", amount)
            // Optional prefill (if you have user data)
            put("prefill", JSONObject().apply {
                put("email", getUserEmail())
                put("contact", getUserPhone())
            })
            put("theme", JSONObject().apply {
                put("color", "#7B68EE")
            })
        }

        checkout.open(this, options)
    }

    private fun getUserEmail(): String {
        // You can read from SharedPreferences or account manager
        return ""
    }

    private fun getUserPhone(): String {
        return ""
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Log.d("Razorpay", "Payment Success: $razorpayPaymentID")

        // Store subscription status locally
        SubscriptionManager.setUserSubscribed(true, razorpayPaymentID)

        runOnUiThread {
            Toast.makeText(this, "Welcome to Premium! 🎉", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        Log.e("Razorpay", "Payment Error: $code - $response")
        runOnUiThread {
            Toast.makeText(this, "Payment failed: $response", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkExistingSubscription() {
        val isSubscribed = SubscriptionManager.isUserSubscribed()
        if (isSubscribed) {
            Toast.makeText(this, "You are already subscribed!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No active subscription found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIForSubscriptionStatus() {
        val isSubscribed = SubscriptionManager.isUserSubscribed()
        val subscribeButton = findViewById<carbon.widget.Button>(R.id.btnSubscribe)

        if (isSubscribed) {
            subscribeButton.text = "Already Subscribed"
            subscribeButton.isEnabled = false
            subscribeButton.alpha = 0.7f
        } else {
            subscribeButton.text = "Buy Subscription"
            subscribeButton.isEnabled = true
            subscribeButton.alpha = 1f
        }
    }
}