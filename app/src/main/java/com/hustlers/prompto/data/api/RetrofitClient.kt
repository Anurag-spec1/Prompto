package com.hustlers.prompto.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://admin.novaprompt.in/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    }
}