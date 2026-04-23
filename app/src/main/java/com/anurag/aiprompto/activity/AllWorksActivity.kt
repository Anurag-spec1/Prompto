package com.anurag.aiprompto.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.anurag.aiprompto.adapter.WorksAdapter
import com.anurag.aiprompto.utils.RecyclerItem
import com.anurag.aiprompto.model.Work
import com.anurag.aiprompto.model.WorkWithImage
import com.anurag.aiprompto.model.WorksResponse
import com.anurag.aiprompto.service.ApiClient
import com.anurag.aiprompto.service.ApiService
import com.anurag.aiprompto.R
import com.anurag.aiprompto.databinding.ActivityAllWorksBinding

class AllWorksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllWorksBinding
    private lateinit var worksAdapter: WorksAdapter
    private val worksList = mutableListOf<WorkWithImage>()
    private lateinit var apiService: ApiService
    private lateinit var loadingDialog: LoadingDialog

    private var currentCategoryId: String = ""
    private var currentCategoryName: String = "All Works"
    private var currentWorkId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllWorksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = ApiClient.getInstance().getApiService()

        getIntentData()
        initializeLoader()
        setupUI()
        setupRecyclerView()
        loadAllWorks()
    }

    private fun getIntentData() {
        currentCategoryId = intent.getStringExtra("CATEGORY_ID") ?: ""
        currentCategoryName = intent.getStringExtra("CATEGORY_NAME") ?: "All Works"
        currentWorkId = intent.getStringExtra("WORK_ID") ?: ""

        Log.d("AllWorksActivity", "Category ID: $currentCategoryId, Category Name: $currentCategoryName")
    }

    private fun setupUI() {
        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.title.text = if (currentCategoryId.isNotEmpty()) {
            currentCategoryName
        } else {
            "All Works"
        }
    }

    private fun setupRecyclerView() {
        worksAdapter = WorksAdapter(emptyList(), this) { recyclerItem ->
            when (recyclerItem) {
                is RecyclerItem.WorkItem -> {
                    val workWithImage = recyclerItem.workWithImage
                    val intent = Intent(this, SelectImage::class.java).apply {
                        putExtra("IMAGE_URL", workWithImage.imageUrl)
                        putExtra("PROMPT_TEXT", workWithImage.work.description)
                        putExtra("WORK_TITLE", workWithImage.work.title)
                        putExtra("CATEGORY_NAME", workWithImage.categoryName)
                        putExtra("CATEGORY_ID", workWithImage.work.categoryId)
                        putExtra("WORK_ID", workWithImage.work.id)
                    }
                    startActivity(intent)
                }
                is RecyclerItem.AdItem -> {
                    Log.d("WorksAdapter", "Ad clicked")
                }
            }
        }
        binding.worksRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.worksRecyclerView.adapter = worksAdapter
    }

    private fun loadAllWorks() {
        showLoader()

        apiService.getAllWorks(1, 100).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                hideLoader()

                if (response.isSuccessful && response.body()?.success == true) {
                    val worksResponse = response.body()!!
                    val allWorks = worksResponse.works?.map { apiWork ->
                        Work(
                            id = apiWork.id,                          // ✅ Was apiWork._id
                            title = apiWork.title,
                            description = apiWork.description,                   // Use title as prompt (or empty string)
                            categoryId = apiWork.categoryId ?: "",    // ✅ categoryId is a String
                            imageUrl = apiWork.imageUrl ?: "",        // ✅ Use imageUrl (from @SerializedName("image_url"))
                            createdAt = "",                           // Not in response
                            updatedAt = "",
                            tags = apiWork.tags ?: emptyList()
                        )
                    } ?: emptyList()

                    val filteredWorks = if (currentCategoryId.isNotEmpty()) {
                        allWorks.filter { work ->
                            work.categoryId == currentCategoryId && work.id != currentWorkId
                        }
                    } else {
                        allWorks
                    }

                    val worksWithImages = filteredWorks.map { work ->
                        WorkWithImage(
                            work = work,
                            categoryName = getCategoryNameFromApiWork(work),
                            imageUrl = work.imageUrl!!
                        )
                    }

                    worksList.clear()
                    worksList.addAll(worksWithImages)

                    val recyclerItems = worksList.map { workWithImage ->
                        RecyclerItem.WorkItem(workWithImage)
                    }
                    worksAdapter.updateItems(recyclerItems)

                    updateEmptyState()

                    Log.d("AllWorksActivity", "Loaded ${worksList.size} works for category: $currentCategoryName")
                } else {
                    showError("Failed to load works")
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                hideLoader()
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun getCategoryNameFromApiWork(work: Work): String {
        return if (currentCategoryId.isNotEmpty()) {
            currentCategoryName
        } else {
            work.categoryId ?: "General"
        }
    }

    private fun updateEmptyState() {
        if (worksList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.worksRecyclerView.visibility = View.GONE

            val emptyStateMessage = if (currentCategoryId.isNotEmpty()) {
                "No works found in $currentCategoryName category"
            } else {
                "No works available"
            }

            binding.emptyState.findViewById<TextView>(R.id.emptyStateText)?.text = emptyStateMessage
        } else {
            binding.emptyState.visibility = View.GONE
            binding.worksRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun initializeLoader() {
        loadingDialog = LoadingDialog(this)
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

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.emptyState.visibility = View.VISIBLE
        binding.worksRecyclerView.visibility = View.GONE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}