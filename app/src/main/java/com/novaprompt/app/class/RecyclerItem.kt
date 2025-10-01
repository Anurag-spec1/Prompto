package com.novaprompt.app.`class`

import com.google.android.gms.ads.nativead.NativeAd
import com.novaprompt.app.model.WorkWithImage

sealed class RecyclerItem {
    data class WorkItem(val workWithImage: WorkWithImage) : RecyclerItem()
    data class AdItem(val nativeAd: NativeAd) : RecyclerItem()
}