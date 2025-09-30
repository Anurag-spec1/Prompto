package com.novaprompt.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.novaprompt.app.R

class WorksShimmerAdapter : RecyclerView.Adapter<WorksShimmerAdapter.ShimmerViewHolder>() {

    inner class ShimmerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shimmerFrameLayout: ShimmerFrameLayout = itemView.findViewById(R.id.shimmer_view_container)

        fun bind() {
            shimmerFrameLayout.startShimmer()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_work_shimmer, parent, false)
        return ShimmerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShimmerViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = 6 // Show 6 shimmer items
}