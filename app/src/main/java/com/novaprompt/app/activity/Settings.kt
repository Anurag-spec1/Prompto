package com.novaprompt.app.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.novaprompt.app.R
import com.novaprompt.app.activity.MainActivity
import com.novaprompt.app.`class`.PrefManager
import com.novaprompt.app.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val adCount = sharedPreferences.getInt("ad_counter", 0) ?: 0
        val prefManager = PrefManager(this)

        prefManager.incrementOpenCount()
        val count = prefManager.getOpenCount()
        Log.d("AppOpenCount", "App opened $count times")
        if (count % adCount == 0) {
            Log.d("AppOpenCount", "This is the $count-th open (multiple of 3)")
            loadInterstitialAd()
            showInterstitialAd()

        }

        binding.back.setOnClickListener {
            onBackPressed()
        }

    }

    private fun getAdsKeys(): Triple<String, String, String> {
        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val bannerAdId = sharedPreferences.getString("banner_ad_id", "ca-app-pub-3940256099942544/6300978111") ?: "ca-app-pub-3940256099942544/6300978111"
        val interstitialAdId = sharedPreferences.getString("interstitial_ad_id", "ca-app-pub-3940256099942544/1033173712") ?: "ca-app-pub-3940256099942544/1033173712"
        val rewardedAdId = sharedPreferences.getString("rewarded_ad_id", "ca-app-pub-3940256099942544/5224354917") ?: "ca-app-pub-3940256099942544/5224354917"

        Log.d("AdVerification", "Banner Ad ID: $bannerAdId")
        Log.d("AdVerification", "Interstitial Ad ID: $interstitialAdId")
        Log.d("AdVerification", "Rewarded Ad ID: $rewardedAdId")

        return Triple(bannerAdId, interstitialAdId, rewardedAdId)
    }

    private fun loadInterstitialAd() {
        try {
            val (bannerAdId, interstitialAdId, rewardedAdId) = getAdsKeys()
            if (isInterstitialLoading) return
            isInterstitialLoading = true

            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                this,
                interstitialAdId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        Log.d("InterstitialAd", "Interstitial ad loaded successfully")

                        // Set up full screen content callback
                        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                Log.d("InterstitialAd", "Interstitial ad dismissed")
                                // Reload for next use
                                loadInterstitialAd()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                                interstitialAd = null
                                Log.e("InterstitialAd", "Interstitial ad failed to show: ${adError.message}")
                                // Reload for next use
                                loadInterstitialAd()
                            }

                            override fun onAdShowedFullScreenContent() {
                                Log.d("InterstitialAd", "Interstitial ad showed")
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.e("InterstitialAd", "Interstitial ad failed to load: ${loadAdError.message}")
                        Toast.makeText(this@Settings, "Interstitial ad failed to load", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isInterstitialLoading = false
        }
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.show(this)
        } else {
            Toast.makeText(this, "Interstitial ad not ready yet", Toast.LENGTH_SHORT).show()
            if (!isInterstitialLoading) {
                loadInterstitialAd()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}