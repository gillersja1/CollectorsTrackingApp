package com.example.collectorstrackingapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.collectorstrackingapp.model.Collectible
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the collectibles table.
 */
@Dao
interface CollectibleDao {
    @Query("SELECT * FROM collectibles ORDER BY timestamp DESC")
    fun getAllCollectibles(): Flow<List<Collectible>>

    @Query("SELECT * FROM collectibles WHERE id = :id")
    fun getCollectible(id: Int): Flow<Collectible>

    @Query("SELECT * FROM collectibles WHERE (:query IS NULL OR name LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') AND (:category IS NULL OR :category = 'All' OR category = :category) ORDER BY timestamp DESC")
    fun searchCollectibles(query: String?, category: String?): Flow<List<Collectible>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectible: Collectible)

    @Delete
    fun delete(collectible: Collectible)
}
