package com.novaprompt.app.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.novaprompt.app.R
import com.novaprompt.app.`class`.SubscriptionManager
import com.novaprompt.app.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private var interstitialAd: InterstitialAd? = null
    private var isUserSubscribed = false
    private var hasCheckedAdInThisSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkSubscriptionStatus()
    }

    override fun onResume() {
        super.onResume()
        if (!hasCheckedAdInThisSession) {
            checkAndShowAdIfNeeded()
            hasCheckedAdInThisSession = true
        }
    }

    private fun setupUI() {
        binding.back.setOnClickListener {
            onBackPressed()
        }
        binding.privacyLayout.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicy::class.java))
        }
        binding.termsLayout.setOnClickListener {
            startActivity(Intent(this, TermsAndConditions::class.java))
        }
        binding.shareLayout.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out ${getString(R.string.app_name)} App")
                putExtra(Intent.EXTRA_TEXT, "Hey! Check out ${getString(R.string.app_name)} app: https://play.google.com/store/apps/details?id=$packageName")
            }

            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        binding.removeAdsLayout.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }

        binding.rateUsLayout.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            startActivity(intent)
        }
        binding.whatsappLayout.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://whatsapp.com/channel/0029Vb6ih32EawdlbxZXrZ3k")
            )
            startActivity(intent)
        }
        binding.customerSupportLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@novaprompt.in"))
                putExtra(Intent.EXTRA_SUBJECT, "Support Request – NovaPrompt")
                putExtra(Intent.EXTRA_TEXT, "")
                `package` = "com.google.android.gm" // Open Gmail app specifically
            }

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Gmail app is not installed.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.appNotWorkingLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("bug@novaprompt.in"))
                putExtra(Intent.EXTRA_SUBJECT, "NovaPrompt Issue / Bug Submission")
                putExtra(Intent.EXTRA_TEXT, "")
                `package` = "com.google.android.gm" // Open Gmail app specifically
            }

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Gmail app is not installed.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun checkAndShowAdIfNeeded() {
        if (isUserSubscribed) {
            Log.d("SettingsAd", "User subscribed, skipping ads")
            return
        }

        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)

        val settingsOpenCounter = sharedPreferences.getInt("settings_open_counter", 0)
        val settingsAdFrequency = 3

        Log.d(
            "SettingsAd",
            "Settings opened $settingsOpenCounter times, showing ad every $settingsAdFrequency openings"
        )

        val newCounter = settingsOpenCounter + 1
        sharedPreferences.edit().putInt("settings_open_counter", newCounter).apply()

        if (newCounter % settingsAdFrequency == 0) {
            Log.d("SettingsAd", "✅ Showing interstitial ad - Settings opened $newCounter times")
            loadAndShowInterstitialAd()
        } else {
            Log.d("SettingsAd", "⏭️ Skipping ad - Settings opened $newCounter times")
        }
    }

    private fun loadAndShowInterstitialAd() {
        Log.d("InterstitialAd", "Starting interstitial ad load for Settings...")

        val (_, interstitialAdId, _) = getAdsKeys()
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            interstitialAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("InterstitialAd", "✅ Interstitial ad loaded successfully")
                    showInterstitialAd()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    Log.e(
                        "InterstitialAd",
                        "❌ Interstitial ad failed to load: ${loadAdError.message}"
                    )
                }
            }
        )
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    Log.d("InterstitialAd", "Interstitial ad dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    Log.e("InterstitialAd", "Interstitial ad failed to show: ${adError.message}")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d("InterstitialAd", "Interstitial ad showed successfully")
                    interstitialAd = null
                }
            }

            interstitialAd?.show(this)
        } else {
            Log.e("InterstitialAd", "❌ Cannot show ad - interstitialAd is null")
        }
    }

    private fun getAdsKeys(): Triple<String, String, String> {
        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)

        val bannerAdId =
            sharedPreferences.getString("banner_ad_id", "ca-app-pub-3940256099942544/6300978111")
                ?: "ca-app-pub-3940256099942544/6300978111"
        val interstitialAdId = sharedPreferences.getString(
            "interstitial_ad_id",
            "ca-app-pub-3940256099942544/1033173712"
        )
            ?: "ca-app-pub-3940256099942544/1033173712"
        val rewardedAdId =
            sharedPreferences.getString("rewarded_ad_id", "ca-app-pub-3940256099942544/5224354917")
                ?: "ca-app-pub-3940256099942544/5224354917"

        Log.d("AdVerification", "Banner Ad ID: $bannerAdId")
        Log.d("AdVerification", "Interstitial Ad ID: $interstitialAdId")
        Log.d("AdVerification", "Rewarded Ad ID: $rewardedAdId")

        return Triple(bannerAdId, interstitialAdId, rewardedAdId)
    }

    private fun checkSubscriptionStatus() {
        SubscriptionManager.checkSubscriptionStatus { subscribed ->
            isUserSubscribed = subscribed
            runOnUiThread {
                if (isUserSubscribed) {
                    Log.d("SettingsAd", "User is subscribed - ads disabled")
                } else {
                    Log.d("SettingsAd", "User is not subscribed - ads enabled")
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        interstitialAd = null
        super.onDestroy()
    }
}