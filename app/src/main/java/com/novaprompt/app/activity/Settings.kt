package com.novaprompt.app.activity

import android.content.Context
import android.health.connect.datatypes.units.Length
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
import com.novaprompt.app.`class`.SubscriptionManager
import com.novaprompt.app.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var isUserSubscribed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkSubscriptionStatus()

        binding.back.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndShowAdIfNeeded()
    }

    private fun checkAndShowAdIfNeeded() {
        if (isUserSubscribed) {
            Log.d("AdCheck", "User is subscribed, skipping ads")
            return
        }

        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val adFrequency = sharedPreferences.getInt("ad_counter", 3) // Default to every 3 opens
        val prefManager = PrefManager(this)

        prefManager.incrementOpenCount()
        val count = prefManager.getOpenCount()

        Log.d("AppOpenCount", "App opened $count times, ad frequency: $adFrequency")

        if (adFrequency > 0 && count % adFrequency == 0) {
            Log.d("AppOpenCount", "Showing interstitial ad on open count: $count")
            loadAndShowInterstitialAd()
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

    private fun loadAndShowInterstitialAd() {
        Toast.makeText(this,"Loaded Interstital ad", Toast.LENGTH_SHORT).show()
        if (isInterstitialLoading) {
            Log.d("InterstitialAd", "Ad is already loading, skipping")
            return
        }

        try {
            val (_, interstitialAdId, _) = getAdsKeys()
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

                        showInterstitialAd()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.e("InterstitialAd", "Interstitial ad failed to load: ${loadAdError.message}")

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
                    Log.d("InterstitialAd", "Interstitial ad showed")
                    interstitialAd = null // Set to null immediately after showing
                }
            }

            interstitialAd?.show(this)
        } else {
            Log.d("InterstitialAd", "Interstitial ad not ready, loading now")
            loadAndShowInterstitialAd()
        }
    }

    private fun checkSubscriptionStatus() {
        SubscriptionManager.checkSubscriptionStatus { subscribed ->
            isUserSubscribed = subscribed
            runOnUiThread {
                if (isUserSubscribed) {
                    hideAds()
                } else {
                    initializeAds()
                }
            }
        }
    }

    private fun initializeAds() {
        loadInterstitialAdForFuture()
    }

    private fun loadInterstitialAdForFuture() {
        if (isInterstitialLoading || interstitialAd != null) {
            return
        }

        try {
            val (_, interstitialAdId, _) = getAdsKeys()
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
                        Log.d("InterstitialAd", "Interstitial ad pre-loaded successfully")

                        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                Log.d("InterstitialAd", "Pre-loaded interstitial ad dismissed")

                                loadInterstitialAdForFuture()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                                interstitialAd = null
                                Log.e("InterstitialAd", "Pre-loaded interstitial ad failed to show: ${adError.message}")

                                loadInterstitialAdForFuture()
                            }

                            override fun onAdShowedFullScreenContent() {
                                Log.d("InterstitialAd", "Pre-loaded interstitial ad showed")
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.e("InterstitialAd", "Interstitial ad pre-load failed: ${loadAdError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isInterstitialLoading = false
        }
    }

    private fun hideAds() {
        Log.d("AdCheck", "Hiding ads for subscribed user")
    }

    private fun loadAd() {
        Log.d("AdCheck", "Loading ads for non-subscribed user")
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