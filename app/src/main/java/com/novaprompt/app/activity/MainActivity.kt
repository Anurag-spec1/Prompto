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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

import com.google.firebase.messaging.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.novaprompt.app.R
import com.novaprompt.app.adapter.CategoriesAdapter
import com.novaprompt.app.adapter.WorksAdapter
import com.novaprompt.app.adapter.WorksShimmerAdapter
import com.novaprompt.app.databinding.ActivityMainBinding
import com.novaprompt.app.model.Category
import com.novaprompt.app.`class`.HorizontalMarginItemDecoration
import com.novaprompt.app.`class`.NativeAdManager
import com.novaprompt.app.`class`.PrefManager
import com.novaprompt.app.`class`.RecyclerItem
import com.novaprompt.app.`class`.SmoothScrollLinearLayoutManager
import com.novaprompt.app.model.CategoriesResponse
import com.novaprompt.app.model.Quadruple
import com.novaprompt.app.model.Work
import com.novaprompt.app.model.WorkWithImage
import com.novaprompt.app.model.WorksResponse
import com.novaprompt.app.service.ApiClient
import retrofit2.Call
import retrofit2.Response
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var worksAdapter: WorksAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var pendingWorkWithImage: WorkWithImage? = null
    private var isInternetAvailable = false
    private var internetDialog: AlertDialog? = null
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var sharedInteger: Int = 0
    private var scrollCounter: Int = 0
    private var isSearchVisible = false
    private lateinit var searchContainer: RelativeLayout

    private lateinit var nativeAdManager: NativeAdManager
    private var nativeAdView: NativeAdView? = null
    private var nativeAdContainer: FrameLayout? = null
    private var isNativeAdShowing = false

    private lateinit var worksShimmerAdapter: WorksShimmerAdapter
    private var isShowingShimmer = false

    private val categoriesList = mutableListOf<Category>()
    private val worksList = mutableListOf<WorkWithImage>()
    private val recyclerItems = mutableListOf<RecyclerItem>()
    private val apiService = ApiClient.getInstance().getApiService()
    private var allWorks = listOf<Work>()
    private var nativeAd: NativeAd? = null
    private var adFrequency = 5

    private var isDataPreloaded = false
    private var shouldRetryLoading = false
    private var isActivityResumed = false
    private var isCurrentlyFiltering = false

    private var originalAllWorks = listOf<Work>()
    private var currentSearchQuery = ""

    private var scrollItemCount = 0
    private var lastAdShownAtItem = -sharedInteger
    private var isUserScrolling = false
    private lateinit var debugTextView: TextView

    private var currentPage = 1
    private val pageSize = 20
    private var isLoadingMore = false
    private var hasMorePages = true
    private var isInitialLoad = true

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

        val sharedPrefer = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        adFrequency = sharedPrefer.getInt("ad_after", 5) ?: 5
        sharedInteger = adFrequency

        searchContainer = binding.searchBarContainer

        setupNativeAd()
        setupRecyclerView()
        setupAdLogic()
        setupSmartAdDisplay()

        val prefManager = PrefManager(this)
        prefManager.incrementOpenCount()
        val count = prefManager.getOpenCount()
        Log.d("AppOpenCount", "App opened $count times")

        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "✅ Subscribed to all_users")
                } else {
                    Log.d("FCM", "❌ Subscription failed")
                }
            }

        setupSearchIcon()
        initializeLoader()
        setupUI()
        setupSearch()
        checkPreloadedData()
        checkInternetAndStart()

        resetScrollCounters()
        setupDebugOverlay()
    }

    private fun setupAdLogic() {
        val prefManager = PrefManager(this)
        prefManager.incrementOpenCount()
        val count = prefManager.getOpenCount()

        val sharedPrefer = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val adCounter = sharedPrefer.getInt("ad_counter", 1) ?: 1

        Log.d("AdCounter", "App opened $count times, showing ad every $adCounter opens")

        if (adCounter > 0 && count % adCounter == 0) {
            Log.d("AdCounter", "✅ Showing interstitial ad on app open")
            loadAndShowInterstitialAdOnAppOpen()
        } else {
            Log.d("AdCounter", "⏭️ Skipping interstitial ad on app open")
            loadInterstitialAd()
        }
    }

    private fun loadAndShowInterstitialAdOnAppOpen() {
        try {
            if (isInterstitialLoading) return
            isInterstitialLoading = true

            val (_, _, interstitialAdId, _) = getAdsKeys()
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                this,
                interstitialAdId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        Log.d("InterstitialAd", "Interstitial ad loaded successfully - Showing immediately on app open")
                        showInterstitialAdOnAppOpen()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.e("InterstitialAd", "Interstitial ad failed to load on app open: ${loadAdError.message}")
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isInterstitialLoading = false
        }
    }

    private fun showInterstitialAdOnAppOpen() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                Log.d("InterstitialAd", "App open interstitial ad dismissed")
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                Log.e("InterstitialAd", "App open interstitial ad failed to show: ${adError.message}")
                loadInterstitialAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("InterstitialAd", "App open interstitial ad showed")
            }
        }

        interstitialAd?.show(this)
    }

    private fun onWorkItemClick(workWithImage: WorkWithImage) {
        pendingWorkWithImage = workWithImage

        if (interstitialAd != null) {
            Log.d("InterstitialAd", "Showing interstitial ad on image click")
            showInterstitialAdOnImageClick()
        } else {
            Log.d("InterstitialAd", "No interstitial ad available, proceeding directly")
            proceedAfterAd()
            loadInterstitialAd()
        }
    }

    private fun showInterstitialAdOnImageClick() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                Log.d("InterstitialAd", "Image click interstitial ad dismissed")
                proceedAfterAd()
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                Log.e("InterstitialAd", "Image click interstitial ad failed to show: ${adError.message}")
                proceedAfterAd()
                loadInterstitialAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("InterstitialAd", "Image click interstitial ad showed")
            }
        }

        interstitialAd?.show(this)
    }

    private fun proceedAfterAd() {
        pendingWorkWithImage?.let { work ->
            showLoader()
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToSelectImage(work)
                hideLoader()
                pendingWorkWithImage = null
            }, 1000)
        }
    }

    private fun navigateToSelectImage(workWithImage: WorkWithImage) {
        val intent = Intent(this, SelectImage::class.java).apply {
            putExtra("IMAGE_URL", workWithImage.imageUrl)
            putExtra("PROMPT_TEXT", workWithImage.work.prompt)
            putExtra("WORK_TITLE", workWithImage.work.title)
            putExtra("CATEGORY_NAME", workWithImage.categoryName)
            putExtra("CATEGORY_ID", workWithImage.work.categoryId)
            putExtra("WORK_ID", workWithImage.work.id)
        }
        startActivity(intent)
    }

    private fun setupSearchIcon() {
        binding.searchIcon.setOnClickListener {
            toggleSearchBar()
        }
    }

    private fun toggleSearchBar() {
        if (isSearchVisible) {
            hideSearchBar()
        } else {
            showSearchBar()
        }
    }

    private fun showSearchBar() {
        isSearchVisible = true
        searchContainer.visibility = View.VISIBLE
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        searchContainer.startAnimation(fadeIn)
        binding.searchEditText.requestFocus()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSearchBar() {
        isSearchVisible = false
        binding.searchEditText.text.clear()
        clearSearch()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)

        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        searchContainer.startAnimation(fadeOut)

        searchContainer.postDelayed({
            searchContainer.visibility = View.GONE
            binding.searchIcon.visibility = View.VISIBLE
        }, 250)
    }

    override fun onBackPressed() {
        if (isSearchVisible) {
            hideSearchBar()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        registerConnectivityCallback()

        if (interstitialAd == null && !isInterstitialLoading) {
            loadInterstitialAd()
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
            private val DELAY: Long = 500

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

    private fun setupNativeAd() {
        nativeAdContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            id = View.generateViewId()
        }

        (binding.root as RelativeLayout).addView(nativeAdContainer)

        val layoutParams = nativeAdContainer?.layoutParams as RelativeLayout.LayoutParams
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        layoutParams.addRule(RelativeLayout.ABOVE, R.id.categoriesRecycler)
        layoutParams.setMargins(16, 0, 16, 16)

        nativeAdManager = NativeAdManager(this)
        loadNativeAdForRecyclerView()
    }

    private fun loadNativeAdForRecyclerView() {
        val (_, nativeAdId, _, _) = getAdsKeys()

        nativeAdManager.loadNativeAd(nativeAdId, object : NativeAdManager.NativeAdListener {
            override fun onAdLoaded(nativeAd: NativeAd) {
                this@MainActivity.nativeAd = nativeAd
                Log.d("NativeAd", "GAM Native ad loaded successfully")

                if (worksList.isNotEmpty()) {
                    runOnUiThread {
                        updateRecyclerViewWithAds(worksList)
                        Log.d("NativeAd", "✅ RecyclerView refreshed with GAM native ads")
                    }
                }
            }

            override fun onAdFailedToLoad(error: String) {
                Log.e("NativeAd", "Failed to load GAM native ad: $error")
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isActivityResumed) {
                        loadNativeAdForRecyclerView()
                    }
                }, 30000)
            }
        })
    }

    private fun resetScrollCounters() {
        scrollItemCount = 0
        lastAdShownAtItem = -sharedInteger
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
        resetPagination()

        val sharedPrefer = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        adFrequency = sharedPrefer.getInt("ad_after", 5) ?: 5
        sharedInteger = adFrequency

        Log.d("AdCounter", "App opened times, showing ad every $adFrequency opens")

        val selectedCategory = categoriesList.find { it.isSelected }
        val worksToDisplay = if (selectedCategory?.name == "Trending 🔥" || selectedCategory == null) {
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
        updateRecyclerViewWithAds(worksWithImages)
        resetScrollCounters()
        showEmptyStateIfNeeded()

        Log.d("TagSearch", "📊 Displaying ${worksList.size} works for category: ${selectedCategory?.name ?: "Trending 🔥"}")
    }

    private fun updateRecyclerViewWithAds(works: List<WorkWithImage>) {
        recyclerItems.clear()

        val sharedPrefer = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val currentAdFrequency = sharedPrefer.getInt("ad_after", 5) ?: 5

        Log.d("AdFrequency", "🔄 Updating RecyclerView - Frequency: $currentAdFrequency, Works: ${works.size}")

        if (works.size < currentAdFrequency) {
            Log.d("AdFrequency", "⏭️ Not enough items for ads (${works.size} < $currentAdFrequency)")
            recyclerItems.addAll(works.map { RecyclerItem.WorkItem(it) })
        } else {
            works.forEachIndexed { index, work ->
                recyclerItems.add(RecyclerItem.WorkItem(work))
                if ((index + 1) % currentAdFrequency == 0 && nativeAd != null && (index + 1) < works.size) {
                    recyclerItems.add(RecyclerItem.AdItem(nativeAd!!))
                    Log.d("AdFrequency", "✅ Added native ad after item ${index + 1}")
                }
            }
            if (nativeAd != null && recyclerItems.none { it is RecyclerItem.AdItem } && works.size >= currentAdFrequency) {
                recyclerItems.add(RecyclerItem.AdItem(nativeAd!!))
                Log.d("AdFrequency", "✅ Added final native ad")
            }
        }

        Log.d("AdFrequency", "📊 Final: ${recyclerItems.size} items (${recyclerItems.count { it is RecyclerItem.WorkItem }} works, ${recyclerItems.count { it is RecyclerItem.AdItem }} ads)")

        worksAdapter.notifyDataSetChanged()
    }

    private fun filterWorksBySearch(query: String) {
        resetPagination()

        if (allWorks.isEmpty()) {
            hideShimmer()
            showEmptyStateIfNeeded()
            return
        }

        showShimmer()
        showLoader()

        val selectedCategory = categoriesList.find { it.isSelected }
        val searchQuery = query.trim().lowercase()

        Log.d("TagSearch", "🔍 Searching tags for: '$searchQuery'")

        val categoryFilteredWorks = if (selectedCategory?.name == "Trending 🔥" || selectedCategory == null) {
            allWorks
        } else {
            allWorks.filter { it.categoryId == selectedCategory.id }
        }

        val searchFilteredWorks = categoryFilteredWorks.filter { work ->
            work.tags.any { tag ->
                tag.trim().lowercase().contains(searchQuery)
            }
        }

        Log.d("TagSearch", "📊 Tag search results: ${searchFilteredWorks.size} out of ${categoryFilteredWorks.size}")

        val sortedFilteredWorks = sortWorksByRecent(searchFilteredWorks)

        val worksWithImages = sortedFilteredWorks.map { work ->
            val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
            WorkWithImage(work, categoryName, work.imageUrl)
        }

        worksList.clear()
        worksList.addAll(worksWithImages)
        updateRecyclerViewWithAds(worksWithImages)

        hideLoader()
        hideShimmer()
        showEmptyStateIfNeeded()

        if (worksList.isEmpty() && query.isNotEmpty()) {
            val categoryText = if (selectedCategory?.name != "Trending 🔥" && selectedCategory != null) {
                " in '${selectedCategory.name}' category"
            } else {
                ""
            }
            binding.emptyStateText.text = "No tags found for \"$query\"$categoryText"
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
        Log.d("FunctionFlow", "📍 checkInternetAndStart() called")
        isInternetAvailable = isInternetConnected()
        Log.d("FunctionFlow", "🌐 Internet available: $isInternetAvailable")

        if (isInternetAvailable) {
            Log.d("FunctionFlow", "🚀 Loading fresh data from API")
            loadDataFromApi()

            if (isDataPreloaded) {
                Log.d("FunctionFlow", "📦 Preloaded data available (fallback only)")
            }
        } else {
            Log.d("FunctionFlow", "❌ No internet connection")
            if (isDataPreloaded) {
                Log.d("FunctionFlow", "📂 Using preloaded data (offline mode)")
                displayPreloadedData()
            } else {
                Log.d("FunctionFlow", "🔄 No data available, showing error")
                showNoInternetDialog()
            }
        }
    }


    private fun getAdsKeys(): Quadruple<String, String, String, String> {
        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)

        val bannerAdId = sharedPreferences.getString("banner_ad_id", "ca-app-pub-3940256099942544/6300978111")
            ?: "ca-app-pub-3940256099942544/6300978111"
        val nativeAdId = sharedPreferences.getString("native_ad_id", "ca-app-pub-3940256099942544/1033173712")
            ?: "ca-app-pub-3940256099942544/1033173712"
        val interstitialAdId = sharedPreferences.getString("interstitial_ad_id", "ca-app-pub-3940256099942544/1033173712")
            ?: "ca-app-pub-3940256099942544/1033173712"
        val rewardedAdId = sharedPreferences.getString("rewarded_ad_id", "ca-app-pub-3940256099942544/5224354917")
            ?: "ca-app-pub-3940256099942544/5224354917"


        Log.d("AdVerification", "Banner Ad ID: $bannerAdId")
        Log.d("AdVerification", "Native Ad ID: $nativeAdId")
        Log.d("AdVerification", "Interstitial Ad ID: $interstitialAdId")
        Log.d("AdVerification", "Rewarded Ad ID: $rewardedAdId")

        return Quadruple(bannerAdId, nativeAdId, interstitialAdId, rewardedAdId)
    }

    private fun loadInterstitialAd() {
        try {
            if (isInterstitialLoading) return
            isInterstitialLoading = true

            val (_, _, interstitialAdId, _) = getAdsKeys()
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                this,
                interstitialAdId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        Log.d("InterstitialAd", "GAM Interstitial ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        Log.e("InterstitialAd", "GAM Interstitial ad failed to load: ${loadAdError.message}")
                        handleGamAdError(loadAdError)
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isInterstitialLoading = false
        }
    }

    private fun handleGamAdError(loadAdError: LoadAdError) {
        when (loadAdError.code) {
            AdRequest.ERROR_CODE_NO_FILL -> {
                Log.w("GAMAd", "Ad request successful, but no ad returned")
            }
            AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                Log.e("GAMAd", "Network error while loading ad")
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

                allWorks = preloadedWorks.map { work ->
                    if (work.tags == null) {
                        work.copy(tags = emptyList())
                    } else {
                        work
                    }
                }

                hideShimmer()
                displayPreloadedData()
            }
        } else {
            showShimmer()
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
            hideShimmer()
            categoriesAdapter.notifyDataSetChanged()
            val worksWithImages = allWorks.map { work ->
                val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
                WorkWithImage(work, categoryName, work.imageUrl)
            }

            worksList.clear()
            worksList.addAll(worksWithImages)
            updateRecyclerViewWithAds(worksWithImages)
            hideLoader()
            showEmptyStateIfNeeded()
            if (!isInternetAvailable) {
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

        worksAdapter = WorksAdapter(recyclerItems, this) { item ->
            when (item) {
                is RecyclerItem.WorkItem -> onWorkItemClick(item.workWithImage)
                is RecyclerItem.AdItem -> {
                    Log.d("AdClick", "Native ad clicked in RecyclerView")
                }
            }
        }

        worksShimmerAdapter = WorksShimmerAdapter()

        binding.worksRecyclerView.layoutManager = GridLayoutManager(this, 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (recyclerItems.getOrNull(position)) {
                        is RecyclerItem.AdItem -> 2
                        else -> 1
                    }
                }
            }
        }

        showShimmer()
        binding.worksRecyclerView.adapter = worksShimmerAdapter

        binding.worksRecyclerView.setHasFixedSize(true)
        binding.worksRecyclerView.setItemViewCacheSize(20)
        binding.worksRecyclerView.isDrawingCacheEnabled = true
        binding.worksRecyclerView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH

        setupPaginationScrollListener()
    }

    private fun setupPaginationScrollListener() {
        binding.worksRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoadingMore && hasMorePages) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0) {
                        loadMoreData()
                    }
                }
            }
        })
    }

    private fun showShimmer() {
        if (!isShowingShimmer) {
            isShowingShimmer = true
            binding.worksRecyclerView.adapter = worksShimmerAdapter
            binding.emptyStateView.visibility = View.GONE
            binding.worksRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun hideShimmer() {
        if (isShowingShimmer) {
            isShowingShimmer = false
            binding.worksRecyclerView.adapter = worksAdapter
        }
    }

    private fun loadDataFromApi() {
        Log.d("FunctionFlow", "📍 loadDataFromApi() called")
        if (!isInternetConnected()) {
            Log.d("FunctionFlow", "❌ No internet in loadDataFromApi")
            isInternetAvailable = false
            showNoInternetDialog()
            shouldRetryLoading = true
            hideShimmer()
            return
        }

        Log.d("FunctionFlow", "✅ Internet confirmed, loading categories...")
        showShimmer()
        resetPagination()
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
        Log.d("FunctionFlow", "📍 loadCategories() called")

        apiService.getAllCategories().enqueue(object : retrofit2.Callback<CategoriesResponse> {
            override fun onResponse(
                call: Call<CategoriesResponse?>,
                response: Response<CategoriesResponse?>
            ) {
                Log.d("FunctionFlow", "📍 Categories API onResponse()")
                Log.d("FunctionFlow", "📡 Categories Response - Success: ${response.isSuccessful}, Body: ${response.body() != null}")

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("FunctionFlow", "✅ Categories loaded successfully")
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
                    categoriesList.add(Category("", "Trending 🔥", "", 0, true))
                    categoriesList.addAll(apiCategories)

                    categoriesAdapter.notifyDataSetChanged()
                    Log.d("FunctionFlow", "🚀 Calling loadWorks() from loadCategories()")
                    loadWorks()
                } else {
                    Log.e("FunctionFlow", "❌ Categories API failed: ${response.code()} - ${response.message()}")
                    handleDataLoadError("Failed to load categories")
                }
            }

            override fun onFailure(call: Call<CategoriesResponse?>, t: Throwable) {
                Log.e("FunctionFlow", "❌ Categories API failure: ${t.message}")
                handleDataLoadError("Network error: ${t.message}")
            }
        })
    }

    private fun loadWorks() {
        Log.d("FunctionFlow", "🎯 loadWorks() FINALLY CALLED!")
        Log.d("FunctionFlow", "📡 Making API call to get works...")

        apiService.getAllWorks(currentPage, pageSize).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: Call<WorksResponse>, response: Response<WorksResponse>) {
                Log.d("FunctionFlow", "📍 Works API onResponse()")
                hideLoader()

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("FunctionFlow", "✅ Works loaded successfully!")
                    val worksResponse = response.body()!!

                    val newWorks = worksResponse.works?.map { apiWork ->
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

                    if (currentPage == 1) {
                        allWorks = newWorks
                    } else {
                        val combinedWorks = allWorks + newWorks
                        allWorks = combinedWorks.distinctBy { it.id }
                    }
                    hasMorePages = newWorks.size == pageSize
                    if (hasMorePages) {
                        currentPage++
                    }

                    if (currentPage == 1) {
                        applyCurrentFilters()
                    } else {
                        updateDisplayWithCurrentData()
                    }

                    isInternetAvailable = true
                    shouldRetryLoading = false
                    saveDataForOfflineUse()

                    Log.d("Pagination", "📄 Page loaded: ${newWorks.size} items, Total: ${allWorks.size}, Has more: $hasMorePages")
                } else {
                    Log.e("FunctionFlow", "❌ Works API failed - Code: ${response.code()}")
                    handleDataLoadError("Failed to load works")
                }
                hideShimmer()
                isLoadingMore = false
            }

            override fun onFailure(call: Call<WorksResponse>, t: Throwable) {
                Log.e("FunctionFlow", "❌ Works API failure: ${t.message}")
                hideLoader()
                hideShimmer()
                isLoadingMore = false
                handleDataLoadError("Network error: ${t.message}")
            }
        })
    }

    private fun loadMoreData() {
        if (isLoadingMore || !hasMorePages || !isInternetAvailable) {
            return
        }

        isLoadingMore = true
        Log.d("Pagination", "🔄 Loading more data - Page: $currentPage")

        apiService.getAllWorks(currentPage, pageSize).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: Call<WorksResponse>, response: Response<WorksResponse>) {
                isLoadingMore = false

                if (response.isSuccessful && response.body()?.success == true) {
                    val worksResponse = response.body()!!
                    val newWorks = worksResponse.works?.map { apiWork ->
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

                    val combinedWorks = allWorks + newWorks
                    allWorks = combinedWorks.distinctBy { it.id }

                    hasMorePages = newWorks.size == pageSize
                    if (hasMorePages) {
                        currentPage++
                    }

                    updateDisplayWithCurrentData()
                    saveDataForOfflineUse()

                    Log.d("Pagination", "✅ More data loaded: ${newWorks.size} items, Total: ${allWorks.size}, Has more: $hasMorePages")
                } else {
                    Log.e("Pagination", "❌ Failed to load more data")
                }
            }

            override fun onFailure(call: Call<WorksResponse>, t: Throwable) {
                isLoadingMore = false
                Log.e("Pagination", "❌ Failed to load more data: ${t.message}")
            }
        })
    }

    private fun updateDisplayWithCurrentData() {
        val selectedCategory = categoriesList.find { it.isSelected }

        val filteredWorks = if (selectedCategory?.name == "Trending 🔥" || selectedCategory == null) {
            allWorks
        } else {
            allWorks.filter { it.categoryId == selectedCategory.id }
        }

        val finalWorks = if (currentSearchQuery.isNotEmpty()) {
            val searchQuery = currentSearchQuery.trim().lowercase()
            filteredWorks.filter { work ->
                work.tags.any { tag ->
                    tag.trim().lowercase().contains(searchQuery)
                }
            }
        } else {
            filteredWorks
        }

        val sortedWorks = sortWorksByRecent(finalWorks)

        val worksWithImages = sortedWorks.map { work ->
            val categoryName = categoriesList.find { it.id == work.categoryId }?.name ?: "Unknown"
            WorkWithImage(work, categoryName, work.imageUrl)
        }

        worksList.clear()
        worksList.addAll(worksWithImages)
        updateRecyclerViewWithAds(worksWithImages)

        showEmptyStateIfNeeded()

        Log.d("Pagination", "🔄 Display updated: ${worksList.size} works")
    }

    private fun showLoadingIndicator() {
    }

    private fun removeLoadingIndicator() {
    }

    private fun resetPagination() {
        currentPage = 1
        isLoadingMore = false
        hasMorePages = true
    }

    private fun applyCurrentFilters() {
        Log.d("TagSearch", "🔄 Applying current filters - Search: '$currentSearchQuery'")
        updateDisplayWithCurrentData()
    }

    private fun handleDataLoadError(errorMessage: String) {
        hideLoader()
        hideShimmer()
        isLoadingMore = false
        removeLoadingIndicator()

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
        resetPagination()

        val sharedPrefer = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        adFrequency = sharedPrefer.getInt("ad_after", 5) ?: 5
        sharedInteger = adFrequency

        showShimmer()
        isCurrentlyFiltering = true

        categoriesList.forEach { it.isSelected = false }
        selectedCategory.isSelected = true
        categoriesAdapter.notifyDataSetChanged()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val categoryFilteredWorks = if (selectedCategory.name == "Trending 🔥") {
                    allWorks
                } else {
                    allWorks.filter { it.categoryId == selectedCategory.id }
                }

                val finalWorks = if (currentSearchQuery.isNotEmpty()) {
                    val searchQuery = currentSearchQuery.trim().lowercase()
                    categoryFilteredWorks.filter { work ->
                        work.tags.any { tag ->
                            tag.trim().lowercase().contains(searchQuery)
                        }
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
                updateRecyclerViewWithAds(worksWithImages)

                binding.worksRecyclerView.post {
                    hideLoader()
                    hideShimmer()
                    showEmptyStateIfNeeded()
                    isCurrentlyFiltering = false
                }
            } catch (e: Exception) {
                hideShimmer()
                isCurrentlyFiltering = false
                Log.e("TagSearch", "Error filtering by category: ${e.message}")
            }
        }, 300)
    }

    private fun showEmptyStateIfNeeded() {
        runOnUiThread {
            if (recyclerItems.isEmpty() || recyclerItems.all { it is RecyclerItem.AdItem}) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.worksRecyclerView.visibility = View.GONE

                val selectedCategory = categoriesList.find { it.isSelected }

                when {
                    currentSearchQuery.isNotEmpty() -> {
                        val categoryText = if (selectedCategory?.name != "Trending 🔥" && selectedCategory != null) {
                            " in '${selectedCategory.name}' category"
                        } else {
                            ""
                        }
                        binding.emptyStateText.text = "No prompts found for \"$currentSearchQuery\"$categoryText"
                    }
                    selectedCategory != null && selectedCategory.name != "Trending 🔥" -> {
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
        resetPagination()
        displayWorksBasedOnCategory()
    }

    private fun loadFreshDataSilently() {
        if (!isInternetConnected()) return

        apiService.getAllCategories().enqueue(object : retrofit2.Callback<CategoriesResponse> {
            override fun onResponse(
                call: Call<CategoriesResponse>,
                response: Response<CategoriesResponse>
            ) {
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

                    if (apiCategories.size != categoriesList.size - 1) {
                        runOnUiThread {
                            categoriesList.clear()
                            categoriesList.add(Category("", "Trending 🔥", "", 0, true))
                            categoriesList.addAll(apiCategories)
                            categoriesAdapter.notifyDataSetChanged()
                        }
                    }
                    apiService.getAllWorks(1, 100).enqueue(object : retrofit2.Callback<WorksResponse> {
                        override fun onResponse(
                            call: Call<WorksResponse>,
                            response: Response<WorksResponse>
                        ) {
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

                        override fun onFailure(call: Call<WorksResponse>, t: Throwable) {
                        }
                    })
                }
            }

            override fun onFailure(call: Call<CategoriesResponse>, t: Throwable) {
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
            resetPagination()
            loadDataFromApi()
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()

            currentCategory?.let {
                filterWorksByCategory(it)
            }
        }
    }

    private fun setupSmartAdDisplay() {
        binding.worksRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastVisibleItemPosition = 0

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        isUserScrolling = true
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        isUserScrolling = false
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 5) {
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val currentVisiblePosition = layoutManager.findLastVisibleItemPosition()

                    if (currentVisiblePosition > lastVisibleItemPosition) {
                        val itemsScrolled = currentVisiblePosition - lastVisibleItemPosition
                        scrollCounter += itemsScrolled
                        lastVisibleItemPosition = currentVisiblePosition

                        Log.d("AdCounter", "Scrolled: $itemsScrolled items, Total: $scrollCounter, Ad Frequency: $adFrequency")
                    }
                }
                updateDebugOverlay()
            }
        })
    }

    private fun setupDebugOverlay() {
        debugTextView = TextView(this).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(16, 8, 16, 8)
            gravity = Gravity.CENTER
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 100.dpToPx()
            marginEnd = 16.dpToPx()
        }

        (binding.root as? ViewGroup)?.addView(debugTextView, params)
        debugTextView.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun updateDebugOverlay() {
        if (BuildConfig.DEBUG) {
            val debugText = """
        📊 Ad Counter Debug
        Ad Frequency: every $adFrequency items
        Current Scroll Count: $scrollCounter
        Native Ad Loaded: ${nativeAd != null}
        Items in RecyclerView: ${recyclerItems.size}
        Pagination: Page $currentPage, Loading: $isLoadingMore, Has More: $hasMorePages
        """.trimIndent()
            debugTextView.text = debugText
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoader()
        internetDialog?.dismiss()
        unregisterConnectivityCallback()
        nativeAd?.destroy()
        nativeAdManager.destroyNativeAd()
        loadingDialog?.dismiss()
    }
}