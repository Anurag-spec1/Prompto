package com.anurag.aiprompto.model

data class AdsKeysResponse(
    val success: Boolean,
    val data: AdsData?
)

data class AdsData(
    val bannerAd: String?,
    val intestrialAd: String?,
    val rewardedAd: String?,
    val nativeAd:String?,
    val adCounter: Int?,
    val adShowAfter:Int?
)
