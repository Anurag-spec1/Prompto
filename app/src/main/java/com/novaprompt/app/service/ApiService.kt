package com.novaprompt.app.service

import com.novaprompt.app.model.CategoriesResponse
import com.novaprompt.app.model.WorksResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/get-all-categories")
    fun getAllCategories(): Call<CategoriesResponse>

    @GET("api/get-all-works")
    fun getAllWorks(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Call<WorksResponse>
}