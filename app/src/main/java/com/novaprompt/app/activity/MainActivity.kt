package com.novaprompt.app.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.messaging.FirebaseMessaging
import com.novaprompt.app.R
import com.novaprompt.app.adapter.CategoriesAdapter
import com.novaprompt.app.adapter.WorksAdapter
import com.novaprompt.app.databinding.ActivityMainBinding
import com.novaprompt.app.model.Category
import com.novaprompt.app.`class`.HorizontalMarginItemDecoration
import com.novaprompt.app.`class`.PrefManager
import com.novaprompt.app.`class`.SmoothScrollLinearLayoutManager
import com.novaprompt.app.model.CategoriesResponse
import com.novaprompt.app.model.Work
import com.novaprompt.app.model.WorkWithImage
import com.novaprompt.app.model.WorksResponse
import com.novaprompt.app.service.ApiClient
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var worksAdapter: WorksAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var isInternetAvailable = false
    private var internetDialog: AlertDialog? = null
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private val categoriesList = mutableListOf<Category>()
    private val worksList = mutableListOf<WorkWithImage>()
    private val apiService = ApiClient.getInstance().getApiService()
    private var allWorks = listOf<Work>()

    private var isDataPreloaded = false
    private var shouldRetryLoading = false
    private var isActivityResumed = false
    private var isCurrentlyFiltering = false

    private var originalAllWorks = listOf<Work>()
    private var currentSearchQuery = ""

    private val connectivityCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (isActivityResumed && !isInternetAvailable) {
                runOnUiThread {
                    isInternetAvailable = true
                    internetDialog?.dismiss()
                    if (shouldRetryLoading) {
                        loadDataFromApi()
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            if (isActivityResumed) {
                runOnUiThread {
                    isInternetAvailable = false
                    shouldRetryLoading = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val adCount = sharedPreferences.getInt("ad_counter", 0) ?: 0
        val prefManager = PrefManager(this)

        prefManager.incrementOpenCount()
        val count = prefManager.getOpenCount()
        Log.d("AppOpenCount", "App opened $count times")
        if (count % adCount == 0) {
            Log.d("AppOpenCount", "This is the $count-th open")
            loadInterstitialAd()
            showInterstitialAd()

        }
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "✅ Subscribed to all_users")
                } else {
                    Log.d("FCM", "❌ Subscription failed")
                }
            }
        initializeLoader()
        setupUI()
        setupSearch()
        setupRecyclerView()
        checkPreloadedData()
        checkInternetAndStart()
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        registerConnectivityCallback()

        if (!isInternetAvailable && isInternetConnected()) {
            isInternetAvailable = true
            internetDialog?.dismiss()
            if (shouldRetryLoading) {
                loadDataFromApi()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        unregisterConnectivityCallback()
    }

    private fun setupSearch() {
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            private var timer: Timer = Timer()
            private val DELAY: Long = 500 // milliseconds

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                timer.cancel()
                timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            performSearch()
                        }
                    }
                }, DELAY)
            }
        })

        binding.searchEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2 // END constant
                if (event.rawX >= (v.right - (binding.searchEditText.compoundDrawables[drawableEnd]?.bounds?.width() ?: 0))) {
                    binding.searchEditText.text.clear()
                    performSearch()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()
        currentSearchQuery = query

        if (query.isEmpty()) {
            displayWorksBasedOnCategory()
        } else {
            filterWorksBySearch(query)
        }
    }

    private fun displayWorksBasedOnCategory() {
        val selectedCategory = categoriesList.find { it.isSelected }
        val worksToDisplay = if (selectedCategory?.name == "All" || selectedCategory == null) {
            allWorks
        } else {
            allWorks.filter { it.categoryId == selectedCategory.id }
        }

        val sortedWorks = sortWorksByRecent(worksToDisplay)

        val worksWithImages = sortedWorks.map { work ->
            val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
            WorkWithImage(work, categoryName, work.imageUrl)
        }

        worksList.clear()
        worksList.addAll(worksWithImages)
        worksAdapter.notifyDataSetChanged()
        showEmptyStateIfNeeded()
    }

    private fun filterWorksBySearch(query: String) {
        if (allWorks.isEmpty()) return

        showLoader()

        val selectedCategory = categoriesList.find { it.isSelected }

        val categoryFilteredWorks = if (selectedCategory?.name == "All" || selectedCategory == null) {
            allWorks
        } else {
            allWorks.filter { it.categoryId == selectedCategory.id }
        }

        val searchFilteredWorks = categoryFilteredWorks.filter { work ->
            work.prompt.contains(query, ignoreCase = true) ||
                    work.title.contains(query, ignoreCase = true)
        }

        val sortedFilteredWorks = sortWorksByRecent(searchFilteredWorks)

        val worksWithImages = sortedFilteredWorks.map { work ->
            val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
            WorkWithImage(work, categoryName, work.imageUrl)
        }

        worksList.clear()
        worksList.addAll(worksWithImages)
        worksAdapter.notifyDataSetChanged()

        hideLoader()
        showEmptyStateIfNeeded()

        if (worksList.isEmpty()) {
            val categoryText = if (selectedCategory?.name != "All" && selectedCategory != null) {
                " in '${selectedCategory.name}' category"
            } else {
                ""
            }
            binding.emptyStateText.text = "No prompts found for \"$query\"$categoryText"
        }
    }



    private fun registerConnectivityCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, connectivityCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterConnectivityCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sortWorksByRecent(works: List<Work>): List<Work> {
        return works.sortedWith(compareByDescending<Work> { work ->
            work.isCreatedInLast24Hours()
        }.thenByDescending { work ->
            work.getCreatedAtDate() ?: Date(0)
        })
    }

    private fun checkInternetAndStart() {
        isInternetAvailable = isInternetConnected()

        if (isInternetAvailable) {
            if (!isDataPreloaded) {
                loadDataFromApi()
            } else {
                displayPreloadedData()
                loadFreshDataSilently()
            }
        } else {
            if (isDataPreloaded) {
                displayPreloadedData()
            } else {
                showNoInternetDialog()
            }
        }
    }

    private fun getAdsKeys(): Triple<String, String, String> {
        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val bannerAdId = sharedPreferences.getString("banner_ad_id", "ca-app-pub-3940256099942544/6300978111") ?: "ca-app-pub-3940256099942544/6300978111"
        val interstitialAdId = sharedPreferences.getString("interstitial_ad_id", "ca-app-pub-3940256099942544/1033173712") ?: "ca-app-pub-3940256099942544/1033173712"
        val rewardedAdId = sharedPreferences.getString("rewarded_ad_id", "ca-app-pub-3940256099942544/5224354917") ?: "ca-app-pub-3940256099942544/5224354917"

        Log.d("AdVerification", "Banner Ad ID: $bannerAdId")
        Log.d("AdVerification", "Interstitial Ad ID: $interstitialAdId")
        Log.d("AdVerification", "Rewarded Ad ID: $rewardedAdId")

        return Triple(bannerAdId, interstitialAdId, rewardedAdId)
    }

    private fun loadInterstitialAd() {
        try {
            val (bannerAdId, interstitialAdId, rewardedAdId) = getAdsKeys()
            if (isInterstitialLoading) return
            isInterstitialLoading = true

            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                this,
                interstitialAdId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        Log.d("InterstitialAd", "Interstitial ad loaded successfully")

                        // Set up full screen content callback
                        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                Log.d("InterstitialAd", "Interstitial ad dismissed")
                                // Reload for next use
                                loadInterstitialAd()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                                interstitialAd = null
                                Log.e("InterstitialAd", "Interstitial ad failed to show: ${adError.message}")
                                // Reload for next use
                                loadInterstitialAd()
                            }

                            override fun onAdShowedFullScreenContent() {
                                Log.d("InterstitialAd", "Interstitial ad showed")
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.e("InterstitialAd", "Interstitial ad failed to load: ${loadAdError.message}")
                        Toast.makeText(this@MainActivity, "Interstitial ad failed to load", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isInterstitialLoading = false
        }
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.show(this)
        } else {
            Toast.makeText(this, "Interstitial ad not ready yet", Toast.LENGTH_SHORT).show()
            // Try to load if not already loading
            if (!isInterstitialLoading) {
                loadInterstitialAd()
            }
        }
    }

    private fun checkPreloadedData() {
        isDataPreloaded = intent.getBooleanExtra("IS_DATA_PRELOADED", false)

        if (isDataPreloaded) {
            val preloadedCategories = intent.getParcelableArrayListExtra<Category>("PRELOADED_CATEGORIES")
            val preloadedWorks = intent.getParcelableArrayListExtra<Work>("PRELOADED_WORKS")

            if (preloadedCategories != null && preloadedWorks != null) {
                categoriesList.clear()
                categoriesList.addAll(preloadedCategories)

                allWorks = preloadedWorks
            }
        }
    }

    private fun isInternetConnected(): Boolean {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities != null && (
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } catch (e: Exception) {
            false
        }
    }

    private fun showNoInternetDialog() {
        if (isDataPreloaded && worksList.isNotEmpty()) {
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, null)

        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)
        val btnRetry = dialogView.findViewById<Button>(R.id.btn_retry)
        val progressBarDialog = dialogView.findViewById<ProgressBar>(R.id.progress_bar_dialog)
        val tvChecking = dialogView.findViewById<TextView>(R.id.tv_checking)

        progressBarDialog.visibility = View.GONE
        tvChecking.visibility = View.GONE
        btnRetry.visibility = View.VISIBLE
        btnClose.visibility = View.VISIBLE

        internetDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        internetDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        internetDialog?.show()

        btnClose.setOnClickListener {
            if (!isDataPreloaded || worksList.isEmpty()) {
                finishAffinity()
            } else {
                internetDialog?.dismiss()
            }
        }

        btnRetry.setOnClickListener {
            checkInternetConnectionWithProgress(dialogView, btnRetry, btnClose, progressBarDialog, tvChecking)
        }
    }

    private fun checkInternetConnectionWithProgress(
        dialogView: View,
        btnRetry: Button,
        btnClose: Button,
        progressBar: ProgressBar,
        tvChecking: TextView
    ) {
        btnRetry.visibility = View.GONE
        btnClose.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        tvChecking.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            if (isInternetConnected()) {
                isInternetAvailable = true
                internetDialog?.dismiss()
                loadDataFromApi()
            } else {
                btnRetry.visibility = View.VISIBLE
                btnClose.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                tvChecking.visibility = View.GONE

                Toast.makeText(this, "No internet connection. Please check your connection and try again.", Toast.LENGTH_SHORT).show()
            }
        }, 1500)
    }

    private fun displayPreloadedData() {
        runOnUiThread {
            categoriesAdapter.notifyDataSetChanged()
            val worksWithImages = allWorks.map { work ->
                val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
                WorkWithImage(work, categoryName, work.imageUrl)
            }

            worksList.clear()
            worksList.addAll(worksWithImages)
            worksAdapter.notifyDataSetChanged()
            hideLoader()
            showEmptyStateIfNeeded()
            if (!isInternetAvailable) {
                Toast.makeText(this, "Using preloaded data. Connect to internet for latest content.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupUI() {
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
            startActivity(Intent(this, Settings::class.java))
        }

        binding.subscription.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }

        setupEmptyStateView()
    }

    private fun setupEmptyStateView() {
        binding.emptyStateView.visibility = View.GONE
        binding.emptyStateText.text = "No prompts found for this category"

    }

    private fun initializeLoader() {
        loadingDialog = LoadingDialog(this)
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(categoriesList) { position ->
            if (isInternetAvailable || isDataPreloaded) {
                val selectedCategory = categoriesList[position]
                filterWorksByCategory(selectedCategory)
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
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

        worksAdapter = WorksAdapter(worksList, this)
        binding.worksRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.worksRecyclerView.adapter = worksAdapter

        binding.worksRecyclerView.setHasFixedSize(true)
        binding.worksRecyclerView.setItemViewCacheSize(20)
        binding.worksRecyclerView.isDrawingCacheEnabled = true
        binding.worksRecyclerView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
    }

    private fun loadDataFromApi() {
        if (!isInternetConnected()) {
            isInternetAvailable = false
            showNoInternetDialog()
            shouldRetryLoading = true
            return
        }

        showLoader()
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

                    categoriesList.clear()
                    categoriesList.add(Category("", "All", "", 0, true))
                    categoriesList.addAll(apiCategories)

                    categoriesAdapter.notifyDataSetChanged()
                    loadWorks()
                } else {
                    handleDataLoadError("Failed to load categories")
                }
            }

            override fun onFailure(call: retrofit2.Call<CategoriesResponse>, t: Throwable) {
                handleDataLoadError("Network error: ${t.message}")
            }
        })
    }

    private fun loadWorks() {
        apiService.getAllWorks(1, 100).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                hideLoader()
//                binding.swipeRefreshLayout.isRefreshing = false

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

                    applyCurrentFilters()

                    isInternetAvailable = true
                    shouldRetryLoading = false
                    saveDataForOfflineUse()
                } else {
                    handleDataLoadError("Failed to load works")
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                hideLoader()
//                binding.swipeRefreshLayout.isRefreshing = false
                handleDataLoadError("Network error: ${t.message}")
            }
        })
    }

    private fun applyCurrentFilters() {
        if (currentSearchQuery.isNotEmpty()) {
            filterWorksBySearch(currentSearchQuery)
        } else {
            displayWorksBasedOnCategory()
        }
    }


    private fun handleDataLoadError(errorMessage: String) {
        hideLoader()

        if (isDataPreloaded && worksList.isEmpty()) {
            displayPreloadedData()
        } else if (!isDataPreloaded || worksList.isEmpty()) {
            showError(errorMessage)
            isInternetAvailable = false
            showNoInternetDialog()
            shouldRetryLoading = true
        } else {
            showError(errorMessage)
        }
    }

    private fun displayAllWorks() {
        displayWorksBasedOnCategory()
    }

    private fun filterWorksByCategory(selectedCategory: Category) {
        if (allWorks.isNotEmpty()) {
            showLoader()
            isCurrentlyFiltering = true
        }

        categoriesList.forEach { it.isSelected = false }
        selectedCategory.isSelected = true
        categoriesAdapter.notifyDataSetChanged()

        Handler(Looper.getMainLooper()).postDelayed({
            val categoryFilteredWorks = if (selectedCategory.name == "All") {
                allWorks
            } else {
                allWorks.filter { it.categoryId == selectedCategory.id }
            }

            val finalWorks = if (currentSearchQuery.isNotEmpty()) {
                categoryFilteredWorks.filter { work ->
                    work.prompt.contains(currentSearchQuery, ignoreCase = true) ||
                            work.title.contains(currentSearchQuery, ignoreCase = true)
                }
            } else {
                categoryFilteredWorks
            }

            val sortedWorks = sortWorksByRecent(finalWorks)

            val worksWithImages = sortedWorks.map { work ->
                val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
                WorkWithImage(work, categoryName, work.imageUrl)
            }

            worksList.clear()
            worksList.addAll(worksWithImages)

            worksAdapter.notifyDataSetChanged()

            binding.worksRecyclerView.post {
                hideLoader()
                showEmptyStateIfNeeded()
                isCurrentlyFiltering = false
            }
        }, 100)
    }

    private fun showEmptyStateIfNeeded() {
        runOnUiThread {
            if (worksList.isEmpty()) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.worksRecyclerView.visibility = View.GONE

                val selectedCategory = categoriesList.find { it.isSelected }

                when {
                    currentSearchQuery.isNotEmpty() -> {
                        val categoryText = if (selectedCategory?.name != "All" && selectedCategory != null) {
                            " in '${selectedCategory.name}' category"
                        } else {
                            ""
                        }
                        binding.emptyStateText.text = "No prompts found for \"$currentSearchQuery\"$categoryText"
                    }
                    selectedCategory != null && selectedCategory.name != "All" -> {
                        binding.emptyStateText.text = "No prompts found for '${selectedCategory.name}' category"
                    }
                    else -> {
                        binding.emptyStateText.text = "No prompts available"
                    }
                }
            } else {
                binding.emptyStateView.visibility = View.GONE
                binding.worksRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    fun clearSearch() {
        binding.searchEditText.text.clear()
        currentSearchQuery = ""
        displayWorksBasedOnCategory()
    }

    private fun loadFreshDataSilently() {
        if (!isInternetConnected()) return

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

                    if (apiCategories.size != categoriesList.size - 1) { // -1 for "All" category
                        runOnUiThread {
                            categoriesList.clear()
                            categoriesList.add(Category("", "All", "", 0, true))
                            categoriesList.addAll(apiCategories)
                            categoriesAdapter.notifyDataSetChanged()
                        }
                    }
                    apiService.getAllWorks(1, 100).enqueue(object : retrofit2.Callback<WorksResponse> {
                        override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                val worksResponse = response.body()!!
                                val freshWorks = worksResponse.works?.map { apiWork ->
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
                                if (freshWorks.size != allWorks.size) {
                                    allWorks = freshWorks
                                    runOnUiThread {
                                        displayAllWorks()
                                        saveDataForOfflineUse()
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                        }
                    })
                }
            }

            override fun onFailure(call: retrofit2.Call<CategoriesResponse>, t: Throwable) {
            }
        })
    }

    private fun saveDataForOfflineUse() {
        val sharedPreferences = getSharedPreferences("app_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("has_offline_data", true)
        editor.putLong("last_data_update", System.currentTimeMillis())
        editor.apply()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun refreshData() {
        val currentCategory = categoriesList.find { it.isSelected }

        clearSearch()

        if (isInternetConnected()) {
//            binding.swipeRefreshLayout.isRefreshing = true
            loadDataFromApi()
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
//            binding.swipeRefreshLayout.isRefreshing = false

            currentCategory?.let {
                filterWorksByCategory(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoader()
        internetDialog?.dismiss()
        unregisterConnectivityCallback()
    }
}