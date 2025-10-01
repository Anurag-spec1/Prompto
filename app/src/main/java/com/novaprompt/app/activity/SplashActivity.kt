package com.novaprompt.app.activity


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.novaprompt.app.R
import com.novaprompt.app.model.AdsData
import com.novaprompt.app.model.AdsKeysResponse
import com.novaprompt.app.model.CategoriesResponse
import com.novaprompt.app.model.Category
import com.novaprompt.app.model.Work
import com.novaprompt.app.model.WorksResponse
import com.novaprompt.app.service.ApiClient
import com.novaprompt.app.service.ApiService


class SplashActivity : AppCompatActivity() {

    private lateinit var logoContainer: View
    private lateinit var logoDownload: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvVersion: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvInitializing: TextView
    private lateinit var bottomFeatures: View

    private val handler = Handler(Looper.getMainLooper())
    private var isInternetAvailable = false
    private var isAnimationCompleted = false
    private var internetDialog: AlertDialog? = null

    private lateinit var apiService: ApiService
    private var preloadedCategories = mutableListOf<Category>()
    private var preloadedWorks = listOf<Work>()
    private var isDataPreloaded = false
    private var adsKeys: AdsData? = null

    // Internet monitoring variables
    private lateinit var connectivityManager: ConnectivityManager
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
    private var isMonitoringInternet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        apiService = ApiClient.getInstance().getApiService()
        val versionCode = getVersionCode()
        initViews()
        tvVersion.text = "⭐ Version $versionCode"
        setupInternetMonitoring()
        checkInternetAndStart()
    }

    private fun initViews() {
        logoContainer = findViewById(R.id.logo_container)
        logoDownload = findViewById(R.id.iv_logo_download)
        tvAppName = findViewById(R.id.tv_app_name)
        tvDescription = findViewById(R.id.tv_description)
        tvVersion = findViewById(R.id.tv_version)
        progressBar = findViewById(R.id.progress_bar)
        tvInitializing = findViewById(R.id.tv_initializing)
        bottomFeatures = findViewById(R.id.bottom_features)
    }

    private fun setupInternetMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("InternetMonitor", "Internet connection available")
                if (!isInternetAvailable) {
                    isInternetAvailable = true
                    handler.post {
                        onInternetRestored()
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("InternetMonitor", "Internet connection lost")
                if (isInternetAvailable) {
                    isInternetAvailable = false
                    handler.post {
                        onInternetLost()
                    }
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.d("InternetMonitor", "Internet connection unavailable")
                if (isInternetAvailable) {
                    isInternetAvailable = false
                    handler.post {
                        onInternetLost()
                    }
                }
            }
        }
    }

    private fun startInternetMonitoring() {
        if (!isMonitoringInternet) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, connectivityCallback!!)
            isMonitoringInternet = true
            Log.d("InternetMonitor", "Started internet monitoring")
        }
    }

    private fun stopInternetMonitoring() {
        if (isMonitoringInternet) {
            connectivityCallback?.let {
                try {
                    connectivityManager.unregisterNetworkCallback(it)
                    isMonitoringInternet = false
                    Log.d("InternetMonitor", "Stopped internet monitoring")
                } catch (e: IllegalArgumentException) {
                    Log.e("InternetMonitor", "Error unregistering network callback: ${e.message}")
                }
            }
        }
    }

    private fun onInternetRestored() {
        Log.d("InternetMonitor", "Internet restored - isAnimationCompleted: $isAnimationCompleted")

        if (progressBar.progress < 100) {
            tvInitializing.text = "Internet restored. Continuing..."
        }

        if (internetDialog != null && internetDialog!!.isShowing) {

            internetDialog?.dismiss()
            preloadData()
        } else if (isAnimationCompleted && !isDataPreloaded) {
             preloadData()
        }
    }

    private fun onInternetLost() {
        Log.d("InternetMonitor", "Internet lost - isAnimationCompleted: $isAnimationCompleted")

        if (isAnimationCompleted) {
            handler.post {
                showNoInternetDialog()
            }
        } else {
            handler.post {
                tvInitializing.text = "Internet connection lost..."
            }
        }
    }

    private fun getVersionCode(): Long {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }

    private fun checkInternetAndStart() {
        isInternetAvailable = isInternetConnected()
        Log.d("SplashActivity", "Initial internet check: $isInternetAvailable")

        startInternetMonitoring()

        if (isInternetAvailable) {
            startSplashAnimation()
            preloadData()
        } else {
            startSplashAnimation()
        }
    }

    private fun preloadData() {
        if (!isInternetAvailable) {
            Log.d("SplashActivity", "No internet available, skipping data preload")
            isDataPreloaded = false
            if (isAnimationCompleted) {
                checkNavigation()
            }
            return
        }

        Log.d("SplashActivity", "Starting data preload")
        fetchAdsKeys()
    }

    private fun fetchAdsKeys() {
        if (!isInternetAvailable) {
            Log.d("SplashActivity", "No internet available, skipping ads keys fetch")
            loadCategoriesDuringSplash()
            return
        }

        apiService.getAllAdsIds().enqueue(object : retrofit2.Callback<AdsKeysResponse> {
            override fun onResponse(call: retrofit2.Call<AdsKeysResponse>, response: retrofit2.Response<AdsKeysResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val adsData = response.body()?.data
                    adsKeys = adsData
                    Log.d("BackendAds", "Backend response: ${adsData?.bannerAd}, ${adsData?.intestrialAd}, ${adsData?.nativeAd}, ${adsData?.rewardedAd},${adsData?.adShowAfter}")
                    saveAdsKeysToSharedPreferences(adsData)
                } else {
                    Log.e("BackendAds", "API call failed: ${response.code()}")
                }
                loadCategoriesDuringSplash()
            }

            override fun onFailure(call: retrofit2.Call<AdsKeysResponse>, t: Throwable) {
                Log.e("BackendAds", "API call error: ${t.message}")
                loadCategoriesDuringSplash()
            }
        })
    }

    private fun saveAdsKeysToSharedPreferences(adsData: AdsData?) {
        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("banner_ad_id", adsData?.bannerAd)
        editor.putString("interstitial_ad_id", adsData?.intestrialAd)
        editor.putString("native_ad_id", adsData?.nativeAd)
        editor.putString("rewarded_ad_id", adsData?.rewardedAd)
        editor.putInt("ad_counter", adsData?.adCounter ?: 0)
        editor.putInt("ad_after", adsData?.adShowAfter ?: 2)
        editor.apply()

        Log.d("BackendAds", "Saved to SharedPreferences: ${adsData?.bannerAd}, ${adsData?.intestrialAd}, ${adsData?.nativeAd}, ${adsData?.rewardedAd},${adsData?.adCounter},${adsData?.adShowAfter}")
    }

    private fun loadCategoriesDuringSplash() {
        if (!isInternetAvailable) {
            Log.d("SplashActivity", "No internet available, skipping categories load")
            isDataPreloaded = false
            if (isAnimationCompleted) {
                checkNavigation()
            }
            return
        }

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

                    preloadedCategories.clear()
                    preloadedCategories.add(Category("", "All", "", 0, true))
                    preloadedCategories.addAll(apiCategories)

                    loadWorksDuringSplash()
                } else {
                    Log.e("SplashActivity", "Categories API call failed: ${response.code()}")
                    isDataPreloaded = false
                    if (isAnimationCompleted) {
                        checkNavigation()
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<CategoriesResponse>, t: Throwable) {
                Log.e("SplashActivity", "Categories API call error: ${t.message}")
                isDataPreloaded = false
                if (isAnimationCompleted) {
                    checkNavigation()
                }
            }
        })
    }

    private fun loadWorksDuringSplash() {
        if (!isInternetAvailable) {
            Log.d("SplashActivity", "No internet available, skipping works load")
            isDataPreloaded = false
            if (isAnimationCompleted) {
                checkNavigation()
            }
            return
        }

        apiService.getAllWorks(1, 100).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val worksResponse = response.body()!!
                    preloadedWorks = worksResponse.works?.map { apiWork ->
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

                    isDataPreloaded = true
                    Log.d("SplashActivity", "Data preload completed successfully")
                    if (isAnimationCompleted) {
                        checkNavigation()
                    }
                } else {
                    Log.e("SplashActivity", "Works API call failed: ${response.code()}")
                    isDataPreloaded = false
                    if (isAnimationCompleted) {
                        checkNavigation()
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                Log.e("SplashActivity", "Works API call error: ${t.message}")
                isDataPreloaded = false
                if (isAnimationCompleted) {
                    checkNavigation()
                }
            }
        })
    }

    private fun checkNavigation() {
        Log.d("SplashActivity", "checkNavigation - Internet: $isInternetAvailable, DataPreloaded: $isDataPreloaded")

        if (isInternetAvailable && isDataPreloaded) {
            Log.d("SplashActivity", "Navigating to MainActivity")
            navigateToMain()
        } else if (!isInternetAvailable) {
            Log.d("SplashActivity", "No internet, showing dialog")
            showNoInternetDialog()
        } else if (isInternetAvailable && !isDataPreloaded) {
            // Internet is available but data preloading failed, retry after delay
            Log.d("SplashActivity", "Data preload failed, retrying...")
            handler.postDelayed({
                preloadData()
            }, 800)
        }
    }

    private fun navigateToMain() {
        Log.d("SplashActivity", "Stopping internet monitoring and navigating to MainActivity")
        stopInternetMonitoring()

        val fadeOut = ObjectAnimator.ofFloat(findViewById<View>(android.R.id.content), "alpha", 1f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        fadeOut.doOnEnd {
            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                putExtra("PRELOADED_CATEGORIES", ArrayList(preloadedCategories))
                putExtra("PRELOADED_WORKS", ArrayList(preloadedWorks))
                putExtra("IS_DATA_PRELOADED", isDataPreloaded)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        fadeOut.start()
    }

    private fun isInternetConnected(): Boolean {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error checking internet connection: ${e.message}")
            false
        }
    }

    private fun startSplashAnimation() {
        Log.d("SplashActivity", "Starting splash animation")
        animateLogo()
        handler.postDelayed({ animateAppName() }, 500)
        handler.postDelayed({ animateDescription() }, 1000)
        handler.postDelayed({ animateVersionBadge() }, 1500)
        handler.postDelayed({ animateProgressBar() }, 2000)
        handler.postDelayed({ animateBottomFeatures() }, 3000)
        handler.postDelayed({
            isAnimationCompleted = true
            Log.d("SplashActivity", "Splash animation completed")
            checkNavigation()
        }, 5000)
    }

    private fun showNoInternetDialog() {
        Log.d("SplashActivity", "Showing no internet dialog")

        // Make sure we're not already showing the dialog
        if (internetDialog != null && internetDialog!!.isShowing) {
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

        if (!isFinishing && !isDestroyed) {
            internetDialog?.show()
        }

        btnClose.setOnClickListener {
            Log.d("SplashActivity", "User chose to close app")
            stopInternetMonitoring()
            finish()
        }

        btnRetry.setOnClickListener {
            Log.d("SplashActivity", "User chose to retry connection")
            if (isInternetConnected()) {
                isInternetAvailable = true
                internetDialog?.dismiss()
                preloadData()
            } else {
                Toast.makeText(this, "No internet connection. Please check your connection and try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun animateLogo() {
        val translateY = ObjectAnimator.ofFloat(logoDownload, "translationY", 50f, 0f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val alpha = ObjectAnimator.ofFloat(logoDownload, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(translateY, alpha)
            start()
        }
    }

    private fun animateAppName() {
        val translateY = ObjectAnimator.ofFloat(tvAppName, "translationY", 50f, 0f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val alpha = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(translateY, alpha)
            start()
        }
    }

    private fun animateDescription() {
        val translateY = ObjectAnimator.ofFloat(tvDescription, "translationY", 50f, 0f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
        }

        val alpha = ObjectAnimator.ofFloat(tvDescription, "alpha", 0f, 1f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(translateY, alpha)
            start()
        }
    }

    private fun animateVersionBadge() {
        val translateY = ObjectAnimator.ofFloat(tvVersion, "translationY", 30f, 0f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        val alpha = ObjectAnimator.ofFloat(tvVersion, "alpha", 0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        val scaleX = ObjectAnimator.ofFloat(tvVersion, "scaleX", 0.8f, 1f).apply {
            duration = 500
            interpolator = BounceInterpolator()
        }

        val scaleY = ObjectAnimator.ofFloat(tvVersion, "scaleY", 0.8f, 1f).apply {
            duration = 500
            interpolator = BounceInterpolator()
        }

        AnimatorSet().apply {
            playTogether(translateY, alpha, scaleX, scaleY)
            start()
        }
    }

    private fun animateProgressBar() {
        val progressAlpha = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }

        val initializingAlpha = ObjectAnimator.ofFloat(tvInitializing, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(progressAlpha, initializingAlpha)
            doOnEnd {
                animateProgress()
            }
            start()
        }
    }

    private fun animateProgress() {
        val progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 2000
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                progressBar.progress = progress

                when (progress) {
                    in 0..20 -> tvInitializing.text = "Initializing..."
                    in 21..40 -> tvInitializing.text = "Loading resources..."
                    in 41..60 -> tvInitializing.text = "Setting up..."
                    in 61..80 -> tvInitializing.text = "Almost ready..."
                    in 81..100 -> tvInitializing.text = if (isInternetAvailable) "Ready!" else "No internet connection"
                }
            }
        }
        progressAnimator.start()
    }

    private fun animateBottomFeatures() {
        val translateY = ObjectAnimator.ofFloat(bottomFeatures, "translationY", 30f, 0f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val alpha = ObjectAnimator.ofFloat(bottomFeatures, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(translateY, alpha)
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SplashActivity", "onDestroy called")
        stopInternetMonitoring()
        handler.removeCallbacksAndMessages(null)
        internetDialog?.dismiss()
    }

    override fun onResume() {
        super.onResume()
        Log.d("SplashActivity", "onResume called")

        isInternetAvailable = isInternetConnected()
        Log.d("SplashActivity", "Internet status on resume: $isInternetAvailable")

        if (internetDialog != null && internetDialog!!.isShowing) {
            if (isInternetAvailable) {
                Log.d("SplashActivity", "Internet available on resume, dismissing dialog")
                isInternetAvailable = true
                internetDialog?.dismiss()
                preloadData()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("SplashActivity", "onPause called")
    }
}