package com.hustlers.prompto.data.api

import com.hustlers.prompto.data.model.CategoryModel
import com.hustlers.prompto.data.model.ImageModel
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("api/get-all-categories")
    suspend fun getAllCategories(): Response<CategoryModel>

    @GET("api/get-all-works")
    suspend fun getAllWorks(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<ImageModel>
}