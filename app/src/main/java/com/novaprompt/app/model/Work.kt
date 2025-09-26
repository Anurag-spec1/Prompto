package com.novaprompt.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Work(
    val id: String,
    val title: String,
    val prompt: String,
    val categoryId: String,
    val imageUrl: String,
    val createdAt: String,
    val updatedAt: String
) : Parcelable
