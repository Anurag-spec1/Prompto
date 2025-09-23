package com.novaprompt.app.model

import com.google.gson.annotations.SerializedName

data class ApiWorkCategory(
    @SerializedName("_id") val _id: String,
    @SerializedName("name") val name: String
)