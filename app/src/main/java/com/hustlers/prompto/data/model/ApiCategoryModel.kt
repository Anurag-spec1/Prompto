package com.hustlers.prompto.data.model

import com.google.gson.annotations.SerializedName

data class ApiCategoryModel(
    @SerializedName("_id") val _id: String,
    @SerializedName("name") val name: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("__v") val __v: Int?
)
