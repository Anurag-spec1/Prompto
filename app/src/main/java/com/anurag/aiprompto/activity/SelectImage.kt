package com.anurag.aiprompto.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.anurag.aiprompto.R
import com.anurag.aiprompto.adapter.WorksAdapter
import com.anurag.aiprompto.utils.RecyclerItem
import com.anurag.aiprompto.utils.SubscriptionManager
import com.anurag.aiprompto.databinding.ActivitySelectImageBinding
import com.anurag.aiprompto.model.Work
import com.anurag.aiprompto.model.WorkWithImage
import com.anurag.aiprompto.model.WorksResponse
import com.anurag.aiprompto.service.ApiClient
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

    private val subscriptionListener = object : SubscriptionManager.SubscriptionListener {
        override fun onSubscriptionStatusChanged(isSubscribed: Boolean) {
            Log.d("SelectImage", "Subscription status changed: $isSubscribed")
            runOnUiThread {
                updateUIForSubscriptionStatus(isSubscribed)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("SelectImage", "Activity created")

        initializeLoader()
        getIntentData()
        setupClickListeners()
        loadImageAndPrompt()
        setupRecyclerView()
        loadRelatedWorks()

        setupSubscription()
    }

    private fun setupSubscription() {
        SubscriptionManager.addSubscriptionListener(subscriptionListener)

        checkSubscriptionStatus()
    }

    private fun checkSubscriptionStatus() {
        Log.d("SelectImage", "Checking subscription status...")
        SubscriptionManager.checkSubscriptionStatus { subscribed ->
            Log.d("SelectImage", "Subscription callback received: $subscribed")
            isUserSubscribed = subscribed
            runOnUiThread {
                updateUIForSubscriptionStatus(subscribed)
            }
        }
    }

    private fun updateUIForSubscriptionStatus(isSubscribed: Boolean) {
        Log.d("SelectImage", "Updating UI for subscription: $isSubscribed")
        this.isUserSubscribed = isSubscribed

        if (isSubscribed) {
            hideAllAds()
            unlockPrompt()
        } else {
//            loadFooterAd()
            lockPrompt()
        }

        updateSubscriptionUI()
    }

    private fun updateSubscriptionUI() {
        if (isUserSubscribed) {
            binding.unlockContainer.visibility = View.GONE
            binding.buttonContainer.visibility = View.VISIBLE
        }
    }

    private fun hideAllAds() {
        Log.d("SelectImage", "Hiding all ads")
//        binding.footerAdView.visibility = View.GONE
        if (::footerAdView.isInitialized) {
            footerAdView.destroy()
        }
        rewardedAd = null
    }

    private fun lockPrompt() {
        if (!isPromptUnlocked && !isUserSubscribed) {
            binding.prompt.text = if (promptText.length > 100) {
                "${promptText.substring(0, 100)}..."
            } else {
                promptText
            }
            binding.buttonContainer.visibility = View.GONE
            binding.unlockContainer.visibility = View.VISIBLE
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

        val spanCount = 2
        binding.relatedWorksRecycler.layoutManager = GridLayoutManager(this, spanCount)
        binding.relatedWorksRecycler.adapter = worksAdapter

        binding.showAllButton.setOnClickListener {
            openAllWorksActivity()
        }
    }

    private fun loadRelatedWorks() {
        binding.relatedWorksSection.visibility = View.VISIBLE

        val currentCategoryId = intent.getStringExtra("CATEGORY_ID") ?: ""
        val currentWorkId = intent.getStringExtra("WORK_ID") ?: ""

        Log.d("RelatedWorks", "Loading works from category: $currentCategoryId, excluding current work: $currentWorkId")

        if (currentCategoryId.isEmpty()) {
            loadAllWorksFallback()
            return
        }

        apiService.getAllWorks(1, 20).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val worksResponse = response.body()!!
                    val allWorks = worksResponse.works?.map { apiWork ->
                        Work(
                            id = apiWork.id,
                            title = apiWork.title,
                            description = apiWork.description,               // or "" if you prefer
                            categoryId = apiWork.categoryId ?: "",
                            imageUrl = apiWork.imageUrl ?: "",
                            createdAt = "",
                            updatedAt = "",
                            tags = apiWork.tags ?: emptyList()
                        )
                    } ?: emptyList()

                    val relatedWorks = allWorks.filter { work ->
                        work.categoryId == currentCategoryId && work.id != currentWorkId
                    }

                    val limitedWorks = relatedWorks.take(6)

                    val worksWithImages = limitedWorks.map { work ->
                        WorkWithImage(
                            work = work,
                            categoryName = work.categoryId ?: "General",
                            imageUrl = work.imageUrl!!
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

                    Log.d("SelectImage", "Displaying ${worksList.size} related works from category: $currentCategoryId")

                    if (worksList.isEmpty()) {
                        loadAllWorksFallback()
                    }
                } else {
                    Log.e("SelectImage", "API response unsuccessful: ${response.code()}")
                    loadAllWorksFallback()
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                Log.e("SelectImage", "Failed to load related works: ${t.message}")
                loadAllWorksFallback()
            }
        })
    }

    private fun loadAllWorksFallback() {
        apiService.getAllWorks(1, 7).enqueue(object : retrofit2.Callback<WorksResponse> {
            override fun onResponse(call: retrofit2.Call<WorksResponse>, response: retrofit2.Response<WorksResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val worksResponse = response.body()!!
                    val works = worksResponse.works?.map { apiWork ->
                        Work(
                            id = apiWork.id,
                            title = apiWork.title,
                            description = apiWork.description,
                            categoryId = apiWork.categoryId ?: "",
                            imageUrl = apiWork.imageUrl ?: "",
                            createdAt = "",
                            updatedAt = "",
                            tags = apiWork.tags ?: emptyList()
                        )
                    } ?: emptyList()

                    val currentWorkId = intent.getStringExtra("WORK_ID") ?: ""
                    val filteredWorks = works.filter { it.id != currentWorkId }.take(6)

                    val worksWithImages = filteredWorks.map { work ->
                        WorkWithImage(
                            work = work,
                            categoryName = work.categoryId ?: "General",
                            imageUrl = work.imageUrl!!
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

                    Log.d("SelectImage", "Fallback: Displaying ${worksList.size} general works")
                } else {
                    binding.relatedWorksSection.visibility = View.GONE
                    binding.showAllButton.visibility = View.GONE
                }
            }

            override fun onFailure(call: retrofit2.Call<WorksResponse>, t: Throwable) {
                binding.relatedWorksSection.visibility = View.GONE
                binding.showAllButton.visibility = View.GONE
            }
        })
    }

    private fun openAllWorksActivity() {
        val intent = Intent(this, AllWorksActivity::class.java).apply {
            putExtra("CATEGORY_ID", intent.getStringExtra("CATEGORY_ID") ?: "")
            putExtra("CATEGORY_NAME", intent.getStringExtra("CATEGORY_NAME") ?: "Related Works")
            putExtra("WORK_ID", intent.getStringExtra("WORK_ID") ?: "")
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun getAdsKeys(): Triple<String, String, String> {
        val sharedPreferences = getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val bannerAdId = sharedPreferences.getString("banner_ad_id", "ca-app-pub-8900849690463057/2912408605") ?: "ca-app-pub-8900849690463057/2912408605"
        val interstitialAdId = sharedPreferences.getString("interstitial_ad_id", "ca-app-pub-8900849690463057/3024089245") ?: "ca-app-pub-8900849690463057/3024089245"
        val rewardedAdId = sharedPreferences.getString("rewarded_ad_id", "ca-app-pub-8900849690463057/3985817126") ?: "ca-app-pub-8900849690463057/3985817126"
        return Triple(bannerAdId, interstitialAdId, rewardedAdId)
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
                        if (!isUserSubscribed) {
                            Log.d("SelectImage", "Rewarded ad loaded successfully")
                            showRewardedAd()
                        } else {
                            unlockPrompt()
                        }
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        isAdLoading = false
                        hideLoader()
                        if (!isUserSubscribed) {
                            Log.e("SelectImage", "Rewarded ad failed to load: ${loadAdError.message}")
                            Toast.makeText(this@SelectImage, "Ad failed to load. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                        unlockPrompt()
                    }
                }
            )
        } catch (e: Exception) {
            isAdLoading = false
            hideLoader()
            Log.e("SelectImage", "Exception loading rewarded ad: ${e.message}")
        }
    }

    private fun showRewardedAd() {
        if (isUserSubscribed) {
            unlockPrompt()
            return
        }

        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d("SelectImage", "Rewarded ad showed full screen content")
                }

                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    Log.d("SelectImage", "Rewarded ad dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.e("SelectImage", "Rewarded ad failed to show: ${adError.message}")
                    unlockPrompt()
                    rewardedAd = null
                }
            }

            rewardedAd?.show(this) { rewardItem ->
                Log.d("SelectImage", "Rewarded ad completed")
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
            if (isUserSubscribed) {
                subscriptionDialog?.dismiss()
                unlockPrompt()
            } else if (rewardedAd != null) {
                subscriptionDialog?.dismiss()
                showRewardedAd()
            } else if (!isAdLoading) {
                Toast.makeText(this, "Loading ad...", Toast.LENGTH_SHORT).show()
                subscriptionDialog?.dismiss()
                loadRewardedAd()
            } else {
                Toast.makeText(this, "Ad is loading...", Toast.LENGTH_SHORT).show()
                subscriptionDialog?.dismiss()
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
        val categoryId = intent.getStringExtra("CATEGORY_ID") ?: ""
        val workId = intent.getStringExtra("WORK_ID") ?: ""
        Log.d("SelectImage", "Received category ID: $categoryId, work ID: $workId")
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
            .placeholder(R.drawable.card_bg)
            .error(R.drawable.card_bg)
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

    override fun onResume() {
        super.onResume()
        Log.d("SelectImage", "Activity resumed, refreshing subscription status")
        SubscriptionManager.refreshSubscriptionStatus()
        checkSubscriptionStatus()
    }

    override fun onBackPressed() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        SubscriptionManager.removeSubscriptionListener(subscriptionListener)
        rewardedAd = null
        if (::footerAdView.isInitialized) {
            footerAdView.destroy()
        }
        loadingDialog?.dismiss()
        Log.d("SelectImage", "Activity destroyed")
    }
}