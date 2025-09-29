package com.novaprompt.app.`class`

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.novaprompt.app.R
import com.novaprompt.app.databinding.LayoutNativeAdBinding

class NativeAdManager(private val context: Context) {

    private var nativeAd: NativeAd? = null
    private var adLoader: AdLoader? = null
    private var isAdLoading = false

    interface NativeAdListener {
        fun onAdLoaded(adView: NativeAdView)
        fun onAdFailedToLoad(error: String)
    }

    fun loadNativeAd(adUnitId: String, listener: NativeAdListener) {
        if (isAdLoading) return

        isAdLoading = true
        Log.d("NativeAd", "Loading native ad...")

        val adRequest = AdRequest.Builder().build()

        adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                Log.d("NativeAd", "Native ad loaded successfully")
                nativeAd?.destroy()
                nativeAd = ad
                isAdLoading = false

                try {
                    val adView = populateNativeAdView(ad)
                    listener.onAdLoaded(adView)
                } catch (e: Exception) {
                    Log.e("NativeAd", "Error creating ad view: ${e.message}")
                    listener.onAdFailedToLoad("Ad view creation failed")
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoading = false
                    Log.e("NativeAd", "Native ad failed to load: ${loadAdError.message}")
                    listener.onAdFailedToLoad(loadAdError.message)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setRequestCustomMuteThisAd(true)
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader?.loadAd(adRequest)
    }

    private fun populateNativeAdView(nativeAd: NativeAd): NativeAdView {
        val binding = LayoutNativeAdBinding.inflate(LayoutInflater.from(context))
        val adView = binding.root

        nativeAd.mediaContent?.let {
            binding.adMedia.mediaContent = it
        }

        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body ?: ""
        binding.adCallToAction.text = nativeAd.callToAction ?: "Learn More"

        if (nativeAd.icon != null) {
            binding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            binding.adAppIcon.visibility = android.view.View.VISIBLE
        } else {
            binding.adAppIcon.visibility = android.view.View.GONE
        }

        binding.adAdvertiser.text = nativeAd.advertiser ?: ""
        binding.close.setOnClickListener {
            nativeAd.destroy()
            (adView.parent as? ViewGroup)?.removeView(adView)
        }

        val adOptionsView = com.google.android.gms.ads.nativead.AdChoicesView(context)
        binding.adChoicesContainer.removeAllViews()
        binding.adChoicesContainer.addView(adOptionsView)

        adView.headlineView = binding.adHeadline
        adView.bodyView = binding.adBody
        adView.callToActionView = binding.adCallToAction
        adView.iconView = binding.adAppIcon
        adView.advertiserView = binding.adAdvertiser
        adView.mediaView = binding.adMedia
        adView.setAdChoicesView(adOptionsView)

        adView.setNativeAd(nativeAd)

        return adView
    }

    fun destroyNativeAd() {
        nativeAd?.destroy()
        nativeAd = null
    }

    fun isAdLoaded(): Boolean = nativeAd != null
}