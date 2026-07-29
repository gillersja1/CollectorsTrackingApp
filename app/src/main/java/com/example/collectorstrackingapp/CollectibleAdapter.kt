package com.example.collectorstrackingapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.collectorstrackingapp.databinding.ItemCollectibleBinding
import com.example.collectorstrackingapp.model.Collectible

class CollectibleAdapter(private val onItemClicked: (Collectible) -> Unit) : ListAdapter<Collectible, CollectibleAdapter.CollectibleViewHolder>(CollectiblesComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectibleViewHolder {
        return CollectibleViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: CollectibleViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
        holder.bind(current)
    }

    class CollectibleViewHolder(private val binding: ItemCollectibleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collectible: Collectible) {
            binding.textViewName.text = collectible.name
            binding.textViewCategory.text = collectible.category
            binding.textViewDescription.text = collectible.description
        }

        companion object {
            fun create(parent: ViewGroup): CollectibleViewHolder {
                val binding = ItemCollectibleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return CollectibleViewHolder(binding)
            }
        }
    }

    class CollectiblesComparator : DiffUtil.ItemCallback<Collectible>() {
        override fun areItemsTheSame(oldItem: Collectible, newItem: Collectible): Boolean {
            return oldItem === newItem || oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Collectible, newItem: Collectible): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.category == newItem.category &&
                    oldItem.description == newItem.description
        }
    }
}
