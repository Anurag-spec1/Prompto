package com.novaprompt.app.model

data class ConditionsResponse(
    val success: Boolean,
    val message: String,
    val data: Data
)

data class Data(
    val _id: String,
    val __v: Int,
    val createdAt: String,
    val url: String
)
