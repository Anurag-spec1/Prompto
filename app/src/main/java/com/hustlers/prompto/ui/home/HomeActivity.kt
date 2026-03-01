package com.hustlers.prompto.ui.home

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.hustlers.prompto.data.api.ApiService
import com.hustlers.prompto.data.api.RetrofitClient
import com.hustlers.prompto.data.model.Work
import com.hustlers.prompto.data.repository.ImageRepository
import com.hustlers.prompto.databinding.ActivityHomeBinding
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(ImageRepository(RetrofitClient.api))
    }

    private lateinit var worksAdapter: WorksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()

        viewModel.loadWorks()
    }

    private fun setupRecyclerView() {
        worksAdapter = WorksAdapter { work ->
            navigateToWorkDetail(work)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = worksAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {

        viewModel.worksLiveData.observe(this, Observer { works ->
            works?.let {
                worksAdapter.submitList(it)
                binding.recyclerView.visibility = View.VISIBLE
                binding.tvError.visibility = View.GONE
            }
        })

        viewModel.errorLiveData.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                binding.recyclerView.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = it
            }
        })
    }

    private fun navigateToWorkDetail(work: Work) {

    }
}