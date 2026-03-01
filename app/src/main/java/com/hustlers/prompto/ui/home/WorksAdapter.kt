package com.hustlers.prompto.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hustlers.prompto.data.model.Work
import com.hustlers.prompto.databinding.ItemWorkBinding

class WorksAdapter(
    private val onClick: (Work) -> Unit
) : RecyclerView.Adapter<WorksAdapter.WorkViewHolder>() {

    private val works = mutableListOf<Work>()

    inner class WorkViewHolder(
        val binding: ItemWorkBinding
    ) : RecyclerView.ViewHolder(binding.root)

    fun submitList(list: List<Work>) {
        works.clear()
        works.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val binding = ItemWorkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {

        val work = works[position]
        val binding = holder.binding

        binding.imageLoading.visibility = View.VISIBLE
        binding.imageErrorLayout.visibility = View.GONE

        Glide.with(binding.root)
            .load(work.imageUrl)
            .thumbnail(0.2f)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.images)

        holder.itemView.setOnClickListener {
            onClick(work)
        }
    }

    override fun getItemCount() = works.size
}