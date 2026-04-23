package com.anurag.aiprompto.service

import com.anurag.aiprompto.model.AdsKeysResponse
import com.anurag.aiprompto.model.BaseResponse
import com.anurag.aiprompto.model.CategoriesResponse
import com.anurag.aiprompto.model.ConditionsResponse
import com.anurag.aiprompto.model.TokenRequest
import com.anurag.aiprompto.model.WorksResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("api/get-all-categories")
    fun getAllCategories(): Call<CategoriesResponse>

    @GET("api/get-privacy-policy")
    fun getConditions(): Call<ConditionsResponse>

    @GET("api/get-keys")
    fun getAllAdsIds(): Call<AdsKeysResponse>

    @GET("api/get-all-works")
    fun getAllWorks(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Call<WorksResponse>

    @GET("api/search-works")
    fun searchWorksByTag(
        @Query("tag") tag: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Call<WorksResponse>

    @POST("api/fcm/register")
    fun registerFCMToken(@Body request: TokenRequest): Call<BaseResponse>
}