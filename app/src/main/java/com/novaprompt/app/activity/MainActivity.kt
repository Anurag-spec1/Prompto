package com.novaprompt.app.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.novaprompt.app.R
import com.novaprompt.app.adapter.CategoriesAdapter
import com.novaprompt.app.adapter.WorksAdapter
import com.novaprompt.app.databinding.ActivityMainBinding
import com.novaprompt.app.model.Category
import com.novaprompt.app.`class`.HorizontalMarginItemDecoration
import com.novaprompt.app.`class`.SmoothScrollLinearLayoutManager
import com.novaprompt.app.model.CategoriesResponse
import com.novaprompt.app.model.Work
import com.novaprompt.app.model.WorkWithImage
import com.novaprompt.app.model.WorksResponse
import com.novaprompt.app.service.ApiClient

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var worksAdapter: WorksAdapter
    private lateinit var loadingDialog: LoadingDialog

    private val categoriesList = mutableListOf<Category>()
    private val worksList = mutableListOf<WorkWithImage>()
    private val apiService = ApiClient.getInstance().getApiService()
    private var allWorks = listOf<Work>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        initializeLoader()
        loadDataFromApi()
    }

    private fun setupUI() {
        // Setup gradient text for logo
        val textShader = LinearGradient(
            0f, 0f, binding.novaPrompt.paint.measureText("NovaPrompt"), 0f,
            intArrayOf(
                Color.parseColor("#00D4FF"),
                Color.parseColor("#7B68EE"),
                Color.parseColor("#FF1493")
            ), null, Shader.TileMode.CLAMP
        )
        binding.novaPrompt.paint.shader = textShader
        binding.novaPrompt.invalidate()

        // Settings button click listener
        binding.settings.setOnClickListener {
            startActivity(Intent(this, SelectImage::class.java))
        }
    }

    private fun initializeLoader() {
        loadingDialog = LoadingDialog(this)
    }

    private fun setupRecyclerView() {
        // Categories RecyclerView
        categoriesAdapter = CategoriesAdapter(categoriesList) { position ->
            val selectedCategory = categoriesList[position]
            filterWorksByCategory(selectedCategory)
        }

        val categoriesLayoutManager = SmoothScrollLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecycler.layoutManager = categoriesLayoutManager
        binding.categoriesRecycler.adapter = categoriesAdapter

        binding.categoriesRecycler.setHasFixedSize(true)
        binding.categoriesRecycler.setItemViewCacheSize(20)

        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
        ).toInt()
        binding.categoriesRecycler.addItemDecoration(HorizontalMarginItemDecoration(margin))

        // Works RecyclerView - Pass context to adapter
        worksAdapter = WorksAdapter(worksList, this)
        binding.worksRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.worksRecyclerView.adapter = worksAdapter

        // Optimize recycler view performance
        binding.worksRecyclerView.setHasFixedSize(true)
        binding.worksRecyclerView.setItemViewCacheSize(20)
        binding.worksRecyclerView.isDrawingCacheEnabled = true
        binding.worksRecyclerView.drawingCacheQuality = android.view.View.DRAWING_CACHE_QUALITY_HIGH
    }

    private fun loadDataFromApi() {
        showLoader()

        // Load categories first
        loadCategories()
    }

    private fun showLoader() {
        if (!loadingDialog.isShowing) {
            loadingDialog.show()
        }
    }

    private fun hideLoader() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun loadCategories() {
        apiService.getAllCategories().enqueue(object : retrofit2.Callback<CategoriesResponse> {
            override fun onResponse(call: retrofit2.Call<CategoriesResponse>, response: retrofit2.Response<CategoriesResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val apiCategories = response.body()!!.data?.map { apiCategory ->
                        Category(
                            id = apiCategory._id,
                            name = apiCategory.name,
                            image = "",
                            count = 0,
                            isSelected = false
                        )
                    } ?: emptyList()

                    // Clear existing data and add "All" category
                    categoriesList.clear()
                    categoriesList.add(Category("", "All", "", 0, true))
                    categoriesList.addAll(apiCategories)

                    categoriesAdapter.notifyDataSetChanged()

                    // Load works after categories are loaded
                    loadWorks()
                } else {
                    hideLoader()
                    showError("Failed to load categories: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<CategoriesResponse>, t: Throwable) {
                hideLoader()
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun loadWorks() {
        apiService.getAllWorks(1, 100).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                hideLoader()

                if (response.isSuccessful && response.body()?.success == true) {
                    val worksResponse = response.body()!!
                    allWorks = worksResponse.works?.map { apiWork ->
                        Work(
                            id = apiWork._id,
                            title = apiWork.prompt ?: "Untitled",
                            prompt = apiWork.prompt ?: "",
                            categoryId = apiWork.categoryId?._id ?: "",
                            imageUrl = apiWork.imageUrl ?: "",
                            createdAt = apiWork.createdAt ?: "",
                            updatedAt = ""
                        )
                    } ?: emptyList()

                    displayAllWorks()
                } else {
                    showError("Failed to load works: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                hideLoader()
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun displayAllWorks() {
        val worksWithImages = allWorks.map { work ->
            val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
            WorkWithImage(work, categoryName, work.imageUrl)
        }

        worksList.clear()
        worksList.addAll(worksWithImages)
        worksAdapter.notifyDataSetChanged()
    }

    private fun filterWorksByCategory(selectedCategory: Category) {
        val filteredWorks = if (selectedCategory.name == "All") {
            allWorks
        } else {
            allWorks.filter { it.categoryId == selectedCategory.id }
        }

        val worksWithImages = filteredWorks.map { work ->
            val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
            WorkWithImage(work, categoryName, work.imageUrl)
        }

        worksList.clear()
        worksList.addAll(worksWithImages)
        worksAdapter.notifyDataSetChanged()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun refreshData() {
        loadDataFromApi()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoader()
    }
}