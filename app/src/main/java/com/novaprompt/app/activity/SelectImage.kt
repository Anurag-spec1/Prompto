package com.novaprompt.app.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.novaprompt.app.R
import com.novaprompt.app.adapter.WorksAdapter
import com.novaprompt.app.`class`.RecyclerItem
import com.novaprompt.app.`class`.SubscriptionManager
import com.novaprompt.app.databinding.ActivitySelectImageBinding
import com.novaprompt.app.model.Work
import com.novaprompt.app.model.WorkWithImage
import com.novaprompt.app.model.WorksResponse
import com.novaprompt.app.service.ApiClient
import java.net.URLEncoder

class SelectImage : AppCompatActivity() {
    private lateinit var binding: ActivitySelectImageBinding
    private var isPromptUnlocked = false
    private var imageUrl: String = ""
    private var promptText: String = ""
    private var workTitle: String = ""
    private var isFooterAdInitialized = false
    private var isFooterAdLoading = false
    private lateinit var loadingDialog: LoadingDialog
    private var rewardedAd: RewardedAd? = null
    private var subscriptionDialog: AlertDialog? = null
    private lateinit var footerAdView: AdView
    private var isAdLoading = false
    private var isUserSubscribed = false

    private lateinit var worksAdapter: WorksAdapter
    private val worksList = mutableListOf<WorkWithImage>()
    private val apiService = ApiClient.getInstance().getApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeLoader()
        getIntentData()
        setupClickListeners()
        loadImageAndPrompt()
        checkSubscriptionStatus()
        setupRecyclerView()
        loadRelatedWorks()
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

        val spanCount = 2
        binding.relatedWorksRecycler.layoutManager = GridLayoutManager(this, spanCount)
        binding.relatedWorksRecycler.adapter = worksAdapter

        binding.showAllButton.setOnClickListener {
            openAllWorksActivity()
        }
    }

    private fun loadRelatedWorks() {
        binding.relatedWorksSection.visibility = View.VISIBLE

        apiService.getAllWorks(1, 7).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
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

                    val limitedWorks = works.take(6)

                    val worksWithImages = limitedWorks.map { work ->
                        WorkWithImage(
                            work = work,
                            categoryName = work.categoryId ?: "General",
                            imageUrl = work.imageUrl
                        )
                    }

                    worksList.clear()
                    worksList.addAll(worksWithImages)

                    val recyclerItems = worksList.map { workWithImage ->
                        RecyclerItem.WorkItem(workWithImage)
                    }
                    worksAdapter.updateItems(recyclerItems)

                    binding.showAllButton.visibility = if (worksList.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.relatedWorksSection.visibility = if (worksList.isNotEmpty()) View.VISIBLE else View.GONE

                    Log.d("SelectImage", "Displaying ${worksList.size} items")
                } else {
                    binding.relatedWorksSection.visibility = View.GONE
                    binding.showAllButton.visibility = View.GONE
                    Log.e("SelectImage", "API response unsuccessful: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                Log.e("SelectImage", "Failed to load related works: ${t.message}")
                binding.relatedWorksSection.visibility = View.GONE
                binding.showAllButton.visibility = View.GONE
            }
        })
    }

    private fun openAllWorksActivity() {
        val intent = Intent(this, AllWorksActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun checkSubscriptionStatus() {
        SubscriptionManager.checkSubscriptionStatus { subscribed ->
            isUserSubscribed = subscribed
            runOnUiThread {
                if (isUserSubscribed) {
                    hideAds()
                    unlockPrompt()
                } else {
//                    loadFooterAd()
                }
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


    private fun loadFooterAd() {
        if (isUserSubscribed) {
            hideAds()
            return
        }

        if (isFooterAdLoading) {
            return
        }

        try {
            val (bannerAdId, _, _) = getAdsKeys()

            if (!isFooterAdInitialized) {
                footerAdView = binding.footerAdView

                if (footerAdView.adUnitId != bannerAdId) {
                    footerAdView.adUnitId = bannerAdId
                }

                footerAdView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        isFooterAdLoading = false
                        Log.d("AdVerification", "✅ Footer banner ad loaded successfully")
                        binding.footerAdView.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isFooterAdLoading = false
                        Log.e("AdVerification", "❌ Footer banner ad failed: ${loadAdError.message}")
                        binding.footerAdView.visibility = View.GONE

//                        Handler(Looper.getMainLooper()).postDelayed({
//                            if (!isUserSubscribed && isActivityResumed && !isFooterAdLoading) {
//                                loadFooterAd()
//                            }
//                        }, 30000)
                    }
                }
                isFooterAdInitialized = true
            }

            if (footerAdView.adUnitId.isNullOrEmpty() || footerAdView.adSize == null) {
                Log.e("AdDebug", "AdView not properly configured, reinitializing...")
                footerAdView.adUnitId = bannerAdId
            }

            isFooterAdLoading = true
            val adRequest = AdRequest.Builder().build()
            footerAdView.loadAd(adRequest)

        } catch (e: Exception) {
            isFooterAdLoading = false
            e.printStackTrace()
            Log.e("AdVerification", "❌ Exception loading footer ad: ${e.message}")
        }
    }

    private fun hideAds() {
        binding.footerAdView.visibility = View.GONE
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

    private fun loadRewardedAd() {
        if (isUserSubscribed) {
            unlockPrompt()
            return
        }

        showLoader()
        try {
            val (_, _, rewardedAdId) = getAdsKeys()
            if (isAdLoading) return
            isAdLoading = true
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                this,
                rewardedAdId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isAdLoading = false
                        hideLoader()
                        showRewardedAd()
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        isAdLoading = false
                        hideLoader()
                        Toast.makeText(this@SelectImage, "Ad failed to load. Please try again.", Toast.LENGTH_SHORT).show()
                        unlockPrompt()
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isAdLoading = false
            hideLoader()
        }
    }

    private fun initializeLoader() {
        loadingDialog = LoadingDialog(this)
    }

    private fun showRewardedAd() {
        if (isUserSubscribed) {
            unlockPrompt()
            return
        }

        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                }

                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    unlockPrompt()
                    rewardedAd = null
                }
            }

            rewardedAd?.show(this) { rewardItem ->
                unlockPrompt()
                subscriptionDialog?.dismiss()
            }
        } else {
            Toast.makeText(this, "Ad not ready. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSubscribe() {
        if (isUserSubscribed) {
            unlockPrompt()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_offers, null)

        val btnSubscribe = dialogView.findViewById<Button>(R.id.btn_subscribe)
        val btnAd = dialogView.findViewById<Button>(R.id.btn_ad)

        subscriptionDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        subscriptionDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        subscriptionDialog?.show()

        subscriptionDialog?.setCanceledOnTouchOutside(true)

        btnAd.setOnClickListener {
            if (rewardedAd != null) {
                subscriptionDialog!!.dismiss()
                showRewardedAd()
            } else if (!isAdLoading) {
                Toast.makeText(this, "Loading ad...", Toast.LENGTH_SHORT).show()
                subscriptionDialog!!.dismiss()
                loadRewardedAd()
            } else {
                Toast.makeText(this, "Ad is loading...", Toast.LENGTH_SHORT).show()
                subscriptionDialog!!.dismiss()
            }
        }

        btnSubscribe.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }
    }

    private fun getIntentData() {
        imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        promptText = intent.getStringExtra("PROMPT_TEXT") ?: "Prompt not available"
        workTitle = intent.getStringExtra("WORK_TITLE") ?: ""
    }

    private fun setupClickListeners() {
        binding.unlockContainer.setOnClickListener {
            if (isUserSubscribed) {
                unlockPrompt()
            } else {
                showSubscribe()
            }
        }

        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.copyButton.setOnClickListener {
            if (isPromptUnlocked || isUserSubscribed) {
                copyPromptToClipboard()
            } else {
                showSubscribe()
            }
        }

        binding.chatgptButton.setOnClickListener {
            if (isPromptUnlocked || isUserSubscribed) {
                openChatGPT()
            } else {
                showSubscribe()
            }
        }

        binding.info.setOnClickListener {
            showImageInfo()
        }

        binding.insta.setOnClickListener {
            shareOnInstagram()
        }
    }

    private fun loadImageAndPrompt() {
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .into(binding.image1)

        binding.prompt.text = if (promptText.length > 100) {
            "${promptText.substring(0, 100)}..."
        } else {
            promptText
        }

        if (isUserSubscribed) {
            unlockPrompt()
        }
    }

    private fun unlockPrompt() {
        binding.prompt.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        binding.prompt.maxLines = 10
        binding.prompt.ellipsize = null
        binding.prompt.text = promptText

        binding.buttonContainer.visibility = android.view.View.VISIBLE
        binding.unlockContainer.visibility = android.view.View.GONE

        isPromptUnlocked = true

        if (!isUserSubscribed) {
            Toast.makeText(this, "Prompt unlocked!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyPromptToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Prompt", promptText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Prompt copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun openChatGPT() {
        try {
            val encodedText = URLEncoder.encode(promptText, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://chat.openai.com/?text=$encodedText")
                setPackage("com.openai.chatgpt")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://chat.openai.com/?text=$encodedText"))
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://chat.openai.com/"))
                    startActivity(webIntent)
                }
            } catch (e2: Exception) {
                Toast.makeText(this, "Could not open ChatGPT", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageInfo() {
        Toast.makeText(this,
            "Image: $workTitle\nCategory: ${intent.getStringExtra("CATEGORY_NAME") ?: "Unknown"}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun shareOnInstagram() {
        try {
            val uri = Uri.parse("https://www.instagram.com/novaprompt_app")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.instagram.android")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val webUri = Uri.parse("https://www.instagram.com/novaprompt_app")
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening Instagram page", Toast.LENGTH_SHORT).show()
        }
    }

    private val isActivityResumed: Boolean
        get() = !isFinishing && !isDestroyed

    override fun onResume() {
        super.onResume()
        checkSubscriptionStatus()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}