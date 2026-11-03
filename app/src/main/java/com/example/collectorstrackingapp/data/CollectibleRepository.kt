package com.example.collectorstrackingapp.data

import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that abstracts access to the data source.
 */
class CollectibleRepository(
    private val collectibleDao: CollectibleDao,
    private val categoryDao: CategoryDao
) {

    val allCollectibles: Flow<List<Collectible>> = collectibleDao.getAllCollectibles()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCollectible(id: Int): Flow<Collectible> {
        return collectibleDao.getCollectible(id)
    }

    fun searchCollectibles(query: String?, category: String?): Flow<List<Collectible>> {
        return collectibleDao.searchCollectibles(query?.let(::escapeLikeWildcards), category)
    }

    /** Saves the item and returns its id (newly generated when the item's id was 0). */
    fun insert(collectible: Collectible): Int {
        return collectibleDao.insert(collectible).toInt()
    }

    fun delete(collectible: Collectible) {
        collectibleDao.delete(collectible)
    }

    fun getAllNow(): List<Collectible> = collectibleDao.getAllNow()

    fun getByCloudId(cloudId: String): Collectible? = collectibleDao.getByCloudId(cloudId)

    /**
     * Marks [uploaded] as safely in the cloud, but only if the row still holds exactly what was
     * uploaded: an edit made while the upload was in flight stays unsynced, so it's sent next time.
     */
    fun markSyncedIfUnchanged(uploaded: Collectible): Boolean {
        val current = collectibleDao.getByCloudId(uploaded.cloudId) ?: return false
        if (current.copy(synced = false) != uploaded.copy(synced = false)) return false
        collectibleDao.markSynced(current.id)
        return true
    }

    /**
     * Deletes copies of the same item, keeping the oldest row (lowest id).
     * Two rows are copies when every field except the id matches; the old
     * sync bug re-imported items with all their fields intact, including the
     * timestamp, so real separate items are never matched.
     * Returns the rows that were deleted.
     */
    fun removeDuplicates(): List<Collectible> {
        val removed = collectibleDao.getAllNow()
            .groupBy { it.copy(id = 0, cloudId = "", synced = false) }
            .values
            .flatMap { copies -> copies.drop(1) }
        removed.forEach { collectibleDao.delete(it) }
        return removed
    }

    fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    companion object {
        /**
         * Escapes SQL LIKE wildcards so user input is matched literally.
         * Without this, searching for "_" or "%" matched every collectible.
         */
        fun escapeLikeWildcards(query: String): String =
            query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
