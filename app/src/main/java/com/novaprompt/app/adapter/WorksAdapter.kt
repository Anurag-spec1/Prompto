package com.novaprompt.app.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.novaprompt.app.R
import com.novaprompt.app.`class`.RecyclerItem
import com.novaprompt.app.databinding.ItemWorkBinding
import com.novaprompt.app.databinding.LayoutNativeAdBinding

class WorksAdapter(
    private var items: List<RecyclerItem>,
    private val context: Context,
    private val onItemClick: (RecyclerItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_WORK = 0
        private const val VIEW_TYPE_AD = 1
    }

    fun updateItems(newItems: List<RecyclerItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    inner class WorkViewHolder(val binding: ItemWorkBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AdViewHolder(val binding: LayoutNativeAdBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RecyclerItem.WorkItem -> VIEW_TYPE_WORK
            is RecyclerItem.AdItem -> VIEW_TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_WORK -> {
                val binding = ItemWorkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                WorkViewHolder(binding)
            }
            VIEW_TYPE_AD -> {
                val binding = LayoutNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WorkViewHolder -> {
                val workItem = items[position] as RecyclerItem.WorkItem
                val workWithImage = workItem.workWithImage

                Glide.with(holder.itemView.context)
                    .load(workWithImage.imageUrl)
                    .placeholder(R.drawable.card_bg)
                    .error(R.drawable.no_internet)
                    .into(holder.binding.images)

                if (workWithImage.work.isCreatedInLast24Hours()) {
                    holder.binding.newImg.visibility = View.VISIBLE
                } else {
                    holder.binding.newImg.visibility = View.GONE
                }

                holder.itemView.setOnClickListener {
                    onItemClick(workItem)
                }
            }
            is AdViewHolder -> {
                val adItem = items[position] as RecyclerItem.AdItem
                val nativeAd = adItem.nativeAd

                populateNativeAdView(nativeAd, holder.binding)

                holder.itemView.setOnClickListener {
                    onItemClick(adItem)
                }
            }
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, binding: LayoutNativeAdBinding) {

        nativeAd.mediaContent?.let {
            binding.adMedia.mediaContent = it
        }

        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body ?: ""
        binding.adCallToAction.text = nativeAd.callToAction ?: "Learn More"
        binding.adAdvertiser.text = nativeAd.advertiser ?: ""

        if (nativeAd.icon != null) {
            binding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            binding.adAppIcon.visibility = View.VISIBLE
        } else {
            binding.adAppIcon.visibility = View.GONE
        }

        binding.close.setOnClickListener {
            Log.d("NativeAd", "Ad closed by user")
        }

        val adOptionsView = com.google.android.gms.ads.nativead.AdChoicesView(context)
        binding.adChoicesContainer.removeAllViews()
        binding.adChoicesContainer.addView(adOptionsView)

        val adView = binding.root
        adView.headlineView = binding.adHeadline
        adView.bodyView = binding.adBody
        adView.callToActionView = binding.adCallToAction
        adView.iconView = binding.adAppIcon
        adView.advertiserView = binding.adAdvertiser
        adView.mediaView = binding.adMedia
        adView.setAdChoicesView(adOptionsView)

        adView.setNativeAd(nativeAd)
    }

    override fun getItemCount(): Int = items.size
}