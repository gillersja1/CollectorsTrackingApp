package com.example.collectorstrackingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.collectorstrackingapp.databinding.ItemCollectibleBinding
import com.example.collectorstrackingapp.model.Collectible
import java.util.Locale

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

            if (collectible.estimatedValue > 0) {
                val formattedVal = if (collectible.estimatedValue % 1.0 == 0.0) {
                    String.format(Locale.US, "$%d", collectible.estimatedValue.toLong())
                } else {
                    String.format(Locale.US, "$%.2f", collectible.estimatedValue)
                }
                binding.textViewValue.text = "Value: $formattedVal"
                binding.textViewValue.visibility = View.VISIBLE
            } else {
                binding.textViewValue.visibility = View.GONE
            }

            if (collectible.isWishlist) {
                binding.textViewWishlist.visibility = View.VISIBLE
            } else {
                binding.textViewWishlist.visibility = View.GONE
            }

            if (collectible.imagePath != null) {
                binding.imageViewCollectible.load(collectible.imagePath) {
                    crossfade(true)
                    placeholder(R.drawable.ic_collectible_placeholder)
                    error(R.drawable.ic_collectible_placeholder)
                }
            } else {
                binding.imageViewCollectible.setImageResource(R.drawable.ic_collectible_placeholder)
            }
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
                    oldItem.description == newItem.description &&
                    oldItem.estimatedValue == newItem.estimatedValue &&
                    oldItem.isWishlist == newItem.isWishlist
        }
    }
}
