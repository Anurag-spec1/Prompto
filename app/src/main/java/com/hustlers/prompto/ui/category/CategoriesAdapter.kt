package com.hustlers.prompto.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hustlers.prompto.R
import com.hustlers.prompto.data.model.Category
import com.hustlers.prompto.databinding.ItemCategoriesBinding

class CategoriesAdapter(
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<Category>()

    inner class CategoryViewHolder(
        val binding: ItemCategoriesBinding
    ) : RecyclerView.ViewHolder(binding.root)

    fun submitList(list: List<Category>) {
        categories.clear()
        categories.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoriesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {

        val category = categories[position]
        val tv = holder.binding.categoryTextView

        tv.text = category.name

        if (category.isSelected) {
            tv.setBackgroundResource(R.color.white)
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.black))
        } else {
            tv.setBackgroundResource(R.drawable.unselected_category)
            tv.setTextColor(ContextCompat.getColor(tv.context, android.R.color.white))
        }

        holder.itemView.setOnClickListener {
            onClick(category)
        }
    }

    override fun getItemCount() = categories.size
}