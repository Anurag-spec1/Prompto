package com.novaprompt.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.novaprompt.app.R
import com.novaprompt.app.databinding.ItemWorkBinding
import com.novaprompt.app.databinding.LayoutNativeAdBinding

class WorksShimmerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_WORK_SHimmer = 0
        private const val VIEW_TYPE_AD_SHimmer = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if ((position + 1) % 5 == 0) VIEW_TYPE_AD_SHimmer else VIEW_TYPE_WORK_SHimmer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD_SHimmer -> {
                val binding = LayoutNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdShimmerViewHolder(binding)
            }
            else -> {
                val binding = ItemWorkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                WorkShimmerViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
         }

    override fun getItemCount(): Int = 10

    inner class WorkShimmerViewHolder(binding: ItemWorkBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AdShimmerViewHolder(binding: LayoutNativeAdBinding) : RecyclerView.ViewHolder(binding.root)
}