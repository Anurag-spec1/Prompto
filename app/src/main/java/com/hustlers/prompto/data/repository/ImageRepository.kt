package com.hustlers.prompto.data.repository

import com.hustlers.prompto.data.api.ApiService
import com.hustlers.prompto.data.model.ImageModel

class ImageRepository(
    private val apiService: ApiService
) {

    suspend fun getWorks(
        page: Int,
        limit: Int
    ): Result<ImageModel> {

        return try {

            val response =
                apiService.getAllWorks(page, limit)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}