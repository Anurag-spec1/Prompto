package com.novaprompt.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String,
    val name: String,
    val image: String,
    val count: Int,
    var isSelected: Boolean
) : Parcelable