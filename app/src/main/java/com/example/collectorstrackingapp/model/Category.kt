package com.example.collectorstrackingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a category for collectibles.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)
