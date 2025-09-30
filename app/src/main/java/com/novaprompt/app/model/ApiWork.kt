package com.novaprompt.app.model

import com.google.gson.annotations.SerializedName

data class ApiWork(
    @SerializedName("_id") val _id: String,
    @SerializedName("categoryId") val categoryId: ApiWorkCategory?,
    @SerializedName("prompt") val prompt: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("tags") val tags: List<String>? = emptyList()
)
