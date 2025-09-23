package com.novaprompt.app.model

data class Category(
    val id: String,
    val name: String,
    val image: String,
    val count: Int,
    var isSelected: Boolean
)