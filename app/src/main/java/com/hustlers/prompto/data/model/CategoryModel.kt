package com.hustlers.prompto.data.model

import com.google.gson.annotations.SerializedName

data class CategoryModel(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ApiCategoryModel>?,
    @SerializedName("cached") val cached: Boolean?
)