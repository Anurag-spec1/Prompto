package com.novaprompt.app.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.novaprompt.app.R
import com.novaprompt.app.activity.SelectImage
import com.novaprompt.app.databinding.ItemWorkBinding
import com.novaprompt.app.model.WorkWithImage

class WorksAdapter(
    private val works: List<WorkWithImage>,
    private val context: Context,
    private val onItemClick: (WorkWithImage) -> Unit
) : RecyclerView.Adapter<WorksAdapter.WorkViewHolder>() {

    inner class WorkViewHolder(val binding: ItemWorkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val binding = ItemWorkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        val workWithImage = works[position]

        Glide.with(holder.itemView.context)
            .load(workWithImage.imageUrl)
            .placeholder(carbon.R.color.carbon_grey_700)
            .error(R.drawable.ic_launcher_foreground)
            .into(holder.binding.images)

        if (workWithImage.work.isCreatedInLast24Hours()) {
            holder.binding.newImg.visibility = View.VISIBLE
        } else {
            holder.binding.newImg.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(workWithImage)
        }
    }

    override fun getItemCount(): Int = works.size
}