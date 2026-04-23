package com.anurag.aiprompto.model

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("os_version") val osVersion: String
)
