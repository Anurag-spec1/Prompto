package com.hustlers.prompto.data.model

import com.google.gson.annotations.SerializedName

data class ApiWorkCategoryModel(
    @SerializedName("_id") val _id: String,
    @SerializedName("name") val name: String
)
