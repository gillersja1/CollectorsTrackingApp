package com.example.collectorstrackingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a registered user in the database.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val passwordHash: String
)
