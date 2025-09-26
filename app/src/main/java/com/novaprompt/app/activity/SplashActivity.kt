package com.novaprompt.app.activity


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        apiService = ApiClient.getInstance().getApiService()
        initViews()
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

    private fun checkInternetAndStart() {
        if (isInternetConnected()) {
            isInternetAvailable = true
            startSplashAnimation()
            preloadData()
        } else {
            isInternetAvailable = false
            startSplashAnimation()
        }
    }

    private fun preloadData() {
        fetchAdsKeys()
    }

    private fun fetchAdsKeys() {
        apiService.getAllAdsIds().enqueue(object : retrofit2.Callback<AdsKeysResponse> {
            override fun onResponse(call: retrofit2.Call<AdsKeysResponse>, response: retrofit2.Response<AdsKeysResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val adsData = response.body()?.data  // Get the AdsData object
                    adsKeys = adsData  // This should work now
                    Log.d("BackendAds", "Backend response: ${adsData?.bannerAd}, ${adsData?.intestrialAd}, ${adsData?.rewardedAd}")
                    saveAdsKeysToSharedPreferences(adsData)  // Pass the data
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
        editor.putString("rewarded_ad_id", adsData?.rewardedAd)
        editor.putInt("ad_counter", adsData?.adCounter ?: 0)
        editor.apply()

        Log.d("BackendAds", "Saved to SharedPreferences: ${adsData?.bannerAd}, ${adsData?.intestrialAd}, ${adsData?.rewardedAd},${adsData?.adCounter}")
    }

    private fun loadCategoriesDuringSplash() {
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
                    isDataPreloaded = false
                    if (isAnimationCompleted) {
                        checkNavigation()
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<CategoriesResponse>, t: Throwable) {
                isDataPreloaded = false
                if (isAnimationCompleted) {
                    checkNavigation()
                }
            }
        })
    }

    private fun loadWorksDuringSplash() {
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
                    if (isAnimationCompleted) {
                        checkNavigation()
                    }
                } else {
                    isDataPreloaded = false
                    if (isAnimationCompleted) {
                        checkNavigation()
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                isDataPreloaded = false
                if (isAnimationCompleted) {
                    checkNavigation()
                }
            }
        })
    }

    private fun checkNavigation() {
        if (isInternetAvailable && isDataPreloaded) {
            navigateToMain()
        } else if (!isInternetAvailable) {
            showNoInternetDialog()
        }
    }

    private fun navigateToMain() {
        val fadeOut = ObjectAnimator.ofFloat(findViewById<View>(android.R.id.content), "alpha", 1f, 0f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        fadeOut.doOnEnd {
            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                putExtra("PRELOADED_CATEGORIES", ArrayList(preloadedCategories))
                putExtra("PRELOADED_WORKS", ArrayList(preloadedWorks))
                putExtra("IS_DATA_PRELOADED", isDataPreloaded)
            }
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        fadeOut.start()
    }

    private fun isInternetConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun startSplashAnimation() {
        animateLogo()
        handler.postDelayed({ animateAppName() }, 800)
        handler.postDelayed({ animateDescription() }, 1300)
        handler.postDelayed({ animateVersionBadge() }, 1800)
        handler.postDelayed({ animateProgressBar() }, 2300)
        handler.postDelayed({ animateBottomFeatures() }, 3500)
        handler.postDelayed({
            isAnimationCompleted = true
            checkNavigation()
        }, 5500)
    }

    private fun showNoInternetDialog() {
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
            finish()
        }

        btnRetry.setOnClickListener {
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
        handler.removeCallbacksAndMessages(null)
        internetDialog?.dismiss()
    }

    override fun onResume() {
        super.onResume()
        if (internetDialog != null && internetDialog!!.isShowing) {
            if (isInternetConnected()) {
                isInternetAvailable = true
                internetDialog?.dismiss()
                preloadData()
            }
        }
    }
}