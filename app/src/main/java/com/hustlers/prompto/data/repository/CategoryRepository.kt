package com.hustlers.prompto.data.repository

import com.hustlers.prompto.data.api.ApiService
import com.hustlers.prompto.data.model.Category
import com.hustlers.prompto.data.model.CategoryModel

class CategoryRepository(
    private val apiService: ApiService
) {

    suspend fun getAllCategories(): Result<CategoryModel> {
        return try {
            val response = apiService.getAllCategories()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}