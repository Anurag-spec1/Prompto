package com.novaprompt.app.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.novaprompt.app.R
import com.novaprompt.app.adapter.WorksAdapter
import com.novaprompt.app.`class`.RecyclerItem
import com.novaprompt.app.databinding.ActivityAllWorksBinding
import com.novaprompt.app.model.Work
import com.novaprompt.app.model.WorkWithImage
import com.novaprompt.app.model.WorksResponse
import com.novaprompt.app.service.ApiClient

class AllWorksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllWorksBinding
    private lateinit var worksAdapter: WorksAdapter
    private val worksList = mutableListOf<WorkWithImage>()
    private val apiService = ApiClient.getInstance().getApiService()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllWorksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeLoader()
        setupUI()
        setupRecyclerView()
        loadAllWorks()
    }

    private fun setupUI() {
        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.title.text = "All Works"
    }

    private fun setupRecyclerView() {
        worksAdapter = WorksAdapter(emptyList(), this) { recyclerItem ->
            when (recyclerItem) {
                is RecyclerItem.WorkItem -> {
                    val workWithImage = recyclerItem.workWithImage
                    val intent = Intent(this, SelectImage::class.java).apply {
                        putExtra("IMAGE_URL", workWithImage.imageUrl)
                        putExtra("PROMPT_TEXT", workWithImage.work.prompt)
                        putExtra("WORK_TITLE", workWithImage.work.title)
                        putExtra("CATEGORY_NAME", workWithImage.categoryName)
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
                    val works = worksResponse.works?.map { apiWork ->
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

                    val worksWithImages = works.map { work ->
                        WorkWithImage(
                            work = work,
                            categoryName = getCategoryNameFromApiWork(work),
                            imageUrl = work.imageUrl
                        )
                    }

                    worksList.clear()
                    worksList.addAll(worksWithImages)

                    val recyclerItems = worksList.map { workWithImage ->
                        RecyclerItem.WorkItem(workWithImage)
                    }
                    worksAdapter.updateItems(recyclerItems)

                    if (worksList.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.worksRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.worksRecyclerView.visibility = View.VISIBLE
                    }
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
        return work.categoryId ?: "General"
    }

    private fun loadAllWorksAlternative() {
        showLoader()

        apiService.getAllWorks(1, 100).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                hideLoader()

                if (response.isSuccessful && response.body()?.success == true) {
                    val worksResponse = response.body()!!
                    val worksWithImages = worksResponse.works?.map { apiWork ->
                        WorkWithImage(
                            work = Work(
                                id = apiWork._id,
                                title = apiWork.prompt ?: "Untitled",
                                prompt = apiWork.prompt ?: "",
                                categoryId = apiWork.categoryId?._id ?: "",
                                imageUrl = apiWork.imageUrl ?: "",
                                createdAt = apiWork.createdAt ?: "",
                                updatedAt = ""
                            ),
                            categoryName = apiWork.categoryId?.name ?: "General", // Use actual category name
                            imageUrl = apiWork.imageUrl ?: ""
                        )
                    } ?: emptyList()

                    worksList.clear()
                    worksList.addAll(worksWithImages)

                    val recyclerItems = worksList.map { workWithImage ->
                        RecyclerItem.WorkItem(workWithImage)
                    }
                    worksAdapter.updateItems(recyclerItems)

                    if (worksList.isEmpty()) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.worksRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.worksRecyclerView.visibility = View.VISIBLE
                    }
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