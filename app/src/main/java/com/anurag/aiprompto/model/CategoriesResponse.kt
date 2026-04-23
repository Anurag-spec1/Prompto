package com.anurag.aiprompto.model

import com.google.gson.annotations.SerializedName

data class CategoriesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ApiCategory>?,
    @SerializedName("cached") val cached: Boolean?
)
