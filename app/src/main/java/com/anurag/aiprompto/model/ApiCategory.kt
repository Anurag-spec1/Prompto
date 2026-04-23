package com.anurag.aiprompto.model

import com.google.gson.annotations.SerializedName

data class ApiCategory(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("order") val order: Int? = null
)