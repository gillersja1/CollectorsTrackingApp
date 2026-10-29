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

    /**
     * [query] is matched literally: callers must escape LIKE wildcards with
     * [CollectibleRepository.escapeLikeWildcards] first.
     */
    @Query("SELECT * FROM collectibles WHERE (:query IS NULL OR name LIKE '%' || :query || '%' ESCAPE '\\' OR tags LIKE '%' || :query || '%' ESCAPE '\\') AND (:category IS NULL OR :category = 'All' OR category = :category) ORDER BY timestamp DESC")
    fun searchCollectibles(query: String?, category: String?): Flow<List<Collectible>>

    /** Returns the row id, which is the id Room generated for a new item. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(collectible: Collectible): Long

    @Delete
    fun delete(collectible: Collectible)
}
