package com.hustlers.prompto.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hustlers.prompto.data.model.ApiCategoryModel
import com.hustlers.prompto.data.model.Category
import com.hustlers.prompto.data.repository.CategoryRepository
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _categoriesLiveData = MutableLiveData<List<Category>>()
    val categoriesLiveData: LiveData<List<Category>> = _categoriesLiveData

    private val _selectedCategoryLiveData = MutableLiveData<Category?>()
    val selectedCategoryLiveData: LiveData<Category?> = _selectedCategoryLiveData

    private val _errorLiveData = MutableLiveData<String>()
    val errorLiveData: LiveData<String> = _errorLiveData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var allCategories = mutableListOf<Category>()

    fun loadCategories() {
        _isLoading.postValue(true)

        viewModelScope.launch {
            val result = repository.getAllCategories()

            result.onSuccess { categoryModel ->
                if (categoryModel.success == true) {
                    val categories = categoryModel.data?.map { apiCategory ->
                        ApiCategoryModel(
                            _id = apiCategory._id,
                            name = apiCategory.name,
                            createdAt = apiCategory.createdAt,
                        )
                    } ?: emptyList()

                    allCategories.clear()
                    allCategories.addAll(categories)
                    _categoriesLiveData.postValue(ArrayList(allCategories))
                }
                _isLoading.postValue(false)
            }.onFailure { exception ->
                _errorLiveData.postValue(exception.message ?: "Failed to load categories")
                _isLoading.postValue(false)
            }
        }
    }

    fun refreshCategories() {
        loadCategories()
    }

    fun selectCategory(category: Category) {
        // Update selection state
        allCategories.forEach {
            it.isSelected = it.id == category.id
        }

        // Find the selected category
        val selectedCategory = allCategories.find { it.id == category.id }

        // Update LiveData
        _categoriesLiveData.postValue(ArrayList(allCategories))
        _selectedCategoryLiveData.postValue(selectedCategory)
    }

    fun getSelectedCategory(): Category? {
        return allCategories.find { it.isSelected }
    }

    fun clearSelection() {
        allCategories.forEach { it.isSelected = false }
        _categoriesLiveData.postValue(ArrayList(allCategories))
        _selectedCategoryLiveData.postValue(null)
    }
}