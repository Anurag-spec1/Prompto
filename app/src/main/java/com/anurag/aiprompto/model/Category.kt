package com.anurag.aiprompto.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String,
    val name: String,
    val image_url: String? = null,   // matches JSON key "image_url"
    val order: Int? = null,          // matches JSON key "order"
    var isSelected: Boolean = false  // default, not from backend
) : Parcelable