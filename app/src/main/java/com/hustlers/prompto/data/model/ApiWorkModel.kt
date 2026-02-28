package com.hustlers.prompto.data.model

import com.google.gson.annotations.SerializedName

data class ApiWorkModel(
    @SerializedName("_id") val _id: String,
    @SerializedName("categoryId") val categoryId: ApiWorkCategoryModel?,
    @SerializedName("prompt") val prompt: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("tags") val tags: List<String>? = emptyList()
)
