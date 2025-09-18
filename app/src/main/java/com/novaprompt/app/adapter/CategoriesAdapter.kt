package com.novaprompt.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.novaprompt.app.R
import com.novaprompt.app.databinding.ItemCategoriesBinding
import com.novaprompt.app.model.Category


class CategoriesAdapter(
    private val categories: MutableList<Category>,
    private val onCategorySelected: (Int) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(val binding: ItemCategoriesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoriesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = CategoryViewHolder(binding)

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                selectCategory(position)
                onCategorySelected(position)
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.categoryTextView.text = category.name

        updateCategoryAppearance(holder.binding.categoryTextView, category.isSelected)
    }

    override fun getItemCount(): Int = categories.size

    private fun selectCategory(position: Int) {
        val previousSelected = categories.indexOfFirst { it.isSelected }
        if (previousSelected != position) {
            if (previousSelected != -1) {
                categories[previousSelected].isSelected = false
                notifyItemChanged(previousSelected)
            }
            categories[position].isSelected = true
            notifyItemChanged(position)
        }
    }

    private fun updateCategoryAppearance(textView: carbon.widget.TextView, isSelected: Boolean) {
        if (isSelected) {
            textView.background = ContextCompat.getDrawable(
                textView.context,
                R.drawable.selected_category_bg
            )
            textView.setTextColor(
                ContextCompat.getColor(
                    textView.context,
                    R.color.selected_category_text
                )
            )
            textView.elevation = 8f
        } else {
            textView.background = ContextCompat.getDrawable(
                textView.context,
                R.drawable.glassmorphism2
            )
            textView.setTextColor(
                ContextCompat.getColor(
                    textView.context,
                    android.R.color.white
                )
            )
            textView.elevation = 0f
        }
    }
}