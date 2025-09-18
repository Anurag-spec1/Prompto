package com.novaprompt.app.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.novaprompt.app.R
import com.novaprompt.app.adapter.CategoriesAdapter
import com.novaprompt.app.databinding.ActivityMainBinding
import com.novaprompt.app.model.Category
import com.novaprompt.app.`class`.HorizontalMarginItemDecoration
import com.novaprompt.app.`class`.SmoothScrollLinearLayoutManager


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var categoriesAdapter: CategoriesAdapter
    private val categoriesList = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.settings.setOnClickListener {
            startActivity(Intent(this, SelectImage::class.java))
        }

        setupCategories()
        setupRecyclerView()
    }

    private fun setupCategories() {
        categoriesList.clear()
        categoriesList.add(Category("All", true))
        categoriesList.add(Category("Technology"))
        categoriesList.add(Category("Science"))
        categoriesList.add(Category("Art"))
        categoriesList.add(Category("Music"))
        categoriesList.add(Category("Sports"))
        categoriesList.add(Category("Food"))
        categoriesList.add(Category("Travel"))
        categoriesList.add(Category("Business"))
        categoriesList.add(Category("Health"))
        categoriesList.add(Category("Education"))
        categoriesList.add(Category("Entertainment"))
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(categoriesList) { position ->
            val selectedCategory = categoriesList[position].name
            Toast.makeText(this, "Selected: $selectedCategory", Toast.LENGTH_SHORT).show()
        }

        val layoutManager =
            SmoothScrollLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecycler.layoutManager = layoutManager
        binding.categoriesRecycler.adapter = categoriesAdapter

        binding.categoriesRecycler.setHasFixedSize(true)
        binding.categoriesRecycler.setItemViewCacheSize(20)
        binding.categoriesRecycler.isDrawingCacheEnabled = true
        binding.categoriesRecycler.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH

        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            resources.displayMetrics
        ).toInt()
        binding.categoriesRecycler.addItemDecoration(HorizontalMarginItemDecoration(margin))

        binding.categoriesRecycler.post {
            binding.categoriesRecycler.smoothScrollToPosition(0)
        }
    }

}