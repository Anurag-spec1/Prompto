package com.anurag.aiprompto.model

import com.google.gson.annotations.SerializedName

data class ApiWork(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("category_id") val categoryId: String? = null
)