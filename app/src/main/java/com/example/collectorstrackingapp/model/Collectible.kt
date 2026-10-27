package com.example.collectorstrackingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a collectible item in the tracking application.
 */
@Entity(tableName = "collectibles")
data class Collectible(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String,
    val description: String,
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isWishlist: Boolean = false,
    val estimatedValue: Double = 0.0,
    val tags: String = ""
)
