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
        return collectibleDao.searchCollectibles(query, category)
    }

    fun insert(collectible: Collectible) {
        collectibleDao.insert(collectible)
    }

    fun delete(collectible: Collectible) {
        collectibleDao.delete(collectible)
    }

    fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }
}
