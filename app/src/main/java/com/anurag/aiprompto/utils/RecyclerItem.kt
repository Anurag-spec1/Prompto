package com.anurag.aiprompto.utils

import com.google.android.gms.ads.nativead.NativeAd
import com.anurag.aiprompto.model.WorkWithImage

sealed class RecyclerItem {
    data class WorkItem(val workWithImage: WorkWithImage) : RecyclerItem()
    data class AdItem(val nativeAd: NativeAd) : RecyclerItem()
}