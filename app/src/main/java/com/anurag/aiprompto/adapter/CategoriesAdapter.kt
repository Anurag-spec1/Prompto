package com.anurag.aiprompto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import carbon.widget.TextView
import com.anurag.aiprompto.R
import com.anurag.aiprompto.databinding.ItemCategoriesBinding
import com.anurag.aiprompto.model.Category

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
        categories.forEachIndexed { index, category ->
            category.isSelected = index == position
        }
        notifyDataSetChanged()
    }

    private fun updateCategoryAppearance(textView: TextView, isSelected: Boolean) {
        if (isSelected) {
            textView.background = ContextCompat.getDrawable(
                textView.context,
                R.color.white

            )
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.black))
            textView.elevation = 8f
        } else {
            textView.background = ContextCompat.getDrawable(
                textView.context,
                R.drawable.unselected_category
            )
            textView.setTextColor(ContextCompat.getColor(textView.context, android.R.color.white))
            textView.elevation = 0f
        }
    }
}