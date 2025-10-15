package com.novaprompt.app.service

import com.novaprompt.app.model.AdsKeysResponse
import com.novaprompt.app.model.BaseResponse
import com.novaprompt.app.model.CategoriesResponse
import com.novaprompt.app.model.ConditionsResponse
import com.novaprompt.app.model.TokenRequest
import com.novaprompt.app.model.WorksResponse
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