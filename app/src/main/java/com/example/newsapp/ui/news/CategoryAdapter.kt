package com.example.newsapp.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp.R
import com.example.newsapp.databinding.ItemCategoryChipBinding
import com.example.newsapp.model.NewsCategory

class CategoryAdapter(
    private val categories: List<NewsCategory>,
    private val onCategorySelected: (NewsCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedIndex: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = categories.size

    private fun updateSelection(newIndex: Int) {
        if (newIndex == selectedIndex) return
        val previousIndex = selectedIndex
        selectedIndex = newIndex
        notifyItemChanged(previousIndex)
        notifyItemChanged(selectedIndex)
        onCategorySelected(categories[selectedIndex])
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.categoryChip.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    updateSelection(position)
                }
            }
        }

        fun bind(category: NewsCategory, isSelected: Boolean) = with(binding.categoryChip) {
            text = category.name
            isChecked = isSelected
            val textColorRes = if (isSelected) R.color.white else R.color.text_secondary
            setTextColor(ContextCompat.getColor(context, textColorRes))
        }
    }

    fun selectCategoryByName(name: String) {
        val index = categories.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index >= 0) {
            updateSelection(index)
        }
    }
}
