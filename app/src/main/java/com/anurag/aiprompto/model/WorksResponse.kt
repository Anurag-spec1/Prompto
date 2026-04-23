package com.anurag.aiprompto.model

import com.google.gson.annotations.SerializedName

data class WorksResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("page") val page: Int?,
    @SerializedName("limit") val limit: Int?,
    @SerializedName("count") val count: Int?,
    @SerializedName("works") val works: List<ApiWork>?
)