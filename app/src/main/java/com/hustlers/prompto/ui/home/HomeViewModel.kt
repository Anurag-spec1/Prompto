package com.hustlers.prompto.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hustlers.prompto.data.model.Work
import com.hustlers.prompto.data.repository.ImageRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    val worksLiveData = MutableLiveData<List<Work>>()
    val errorLiveData = MutableLiveData<String>()

    private var currentPage = 1
    private val pageSize = 10
    private val allWorks = mutableListOf<Work>()

    fun loadWorks() {

        viewModelScope.launch {

            val result =
                repository.getWorks(currentPage, pageSize)

            result.onSuccess { worksResponse ->

                if (worksResponse.success == true) {

                    val newWorks =
                        worksResponse.works?.map { apiWork ->

                            Work(
                                id = apiWork._id,
                                title = apiWork.prompt ?: "Untitled",
                                prompt = apiWork.prompt ?: "",
                                categoryId = apiWork.categoryId?._id ?: "",
                                imageUrl = apiWork.imageUrl ?: "",
                                createdAt = apiWork.createdAt ?: "",
                                updatedAt = "",
                                tags = apiWork.tags ?: emptyList()
                            )
                        } ?: emptyList()

                    allWorks.addAll(newWorks)

                    worksLiveData.postValue(allWorks)

                    if (newWorks.size == pageSize) {
                        currentPage++
                    }
                }

            }.onFailure {
                errorLiveData.postValue(it.message)
            }
        }
    }
}