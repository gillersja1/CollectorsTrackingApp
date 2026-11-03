package com.example.collectorstrackingapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a collectible item in the tracking application.
 *
 * [id] is this phone's own row number. [cloudId] names the item everywhere else: it's the
 * Firestore document id, random so two phones can never pick the same one. [synced] is true
 * once the cloud has confirmed it holds this version of the item.
 */
@Entity(tableName = "collectibles", indices = [Index(value = ["cloudId"], unique = true)])
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
    val tags: String = "",
    @ColumnInfo(defaultValue = "")
    val cloudId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val synced: Boolean = false
)
