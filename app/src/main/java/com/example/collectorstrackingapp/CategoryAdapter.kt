package com.example.collectorstrackingapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorstrackingapp.databinding.ItemCategoryBinding
import com.example.collectorstrackingapp.model.Category

class CategoryAdapter(
    private val onItemClicked: (Category) -> Unit,
    private val onDeleteClicked: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoriesComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        return CategoryViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, onItemClicked, onDeleteClicked)
    }

    class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            category: Category,
            onItemClicked: (Category) -> Unit,
            onDeleteClicked: (Category) -> Unit
        ) {
            binding.textViewCategoryName.text = category.name
            binding.textViewCategoryName.setOnClickListener {
                onItemClicked(category)
            }
            binding.buttonDeleteCategory.setOnClickListener {
                onDeleteClicked(category)
            }
        }

        companion object {
            fun create(parent: ViewGroup): CategoryViewHolder {
                val binding = ItemCategoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return CategoryViewHolder(binding)
            }
        }
    }

    class CategoriesComparator : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.name == newItem.name
        }
    }
}
