package com.novaprompt.app.`class`

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd

class NativeAdManager(private val context: Context) {
    private var nativeAd: NativeAd? = null
    private var isAdLoading = false
    private var retryCount = 0
    private val maxRetries = 3

    interface NativeAdListener {
        fun onAdLoaded(nativeAd: NativeAd)
        fun onAdFailedToLoad(error: String)
    }

    fun loadNativeAd(adUnitId: String, listener: NativeAdListener) {
        if (isAdLoading) {
            Log.d("NativeAd", "Ad loading already in progress, skipping...")
            return
        }

        isAdLoading = true
        retryCount++
        Log.d("NativeAd", "Loading native ad... (Attempt $retryCount/$maxRetries)")

        val adRequest = AdRequest.Builder().build()

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                Log.d("NativeAd", "✅ Native ad loaded successfully on attempt $retryCount")
                nativeAd?.destroy()
                nativeAd = ad
                isAdLoading = false
                retryCount = 0
                listener.onAdLoaded(ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    Log.e("NativeAd", "❌ Native ad failed to load: ${loadAdError.message}")

                    if (retryCount < maxRetries) {
                        val retryDelay = calculateRetryDelay(retryCount)
                        Log.d("NativeAd", "🔄 Retrying in ${retryDelay/1000} seconds...")

                        Handler(Looper.getMainLooper()).postDelayed({
                            if (context is Activity && !context.isDestroyed) {
                                loadNativeAd(adUnitId, listener)
                            }
                        }, retryDelay)
                    } else {
                        Log.e("NativeAd", "❌ Max retries reached ($maxRetries), giving up")
                        retryCount = 0
                        listener.onAdFailedToLoad(loadAdError.message)
                    }
                }
            })
            .build()

        adLoader.loadAd(adRequest)
    }

    private fun calculateRetryDelay(attempt: Int): Long {
        return when (attempt) {
            1 -> 10000L
            2 -> 20000L
            3 -> 30000L
            else -> 30000L
        }
    }

    fun getLoadedNativeAd(): NativeAd? = nativeAd

    fun destroyNativeAd() {
        nativeAd?.destroy()
        nativeAd = null
        isAdLoading = false
        retryCount = 0
    }

    fun isAdLoading(): Boolean = isAdLoading
    fun hasAdLoaded(): Boolean = nativeAd != null
}