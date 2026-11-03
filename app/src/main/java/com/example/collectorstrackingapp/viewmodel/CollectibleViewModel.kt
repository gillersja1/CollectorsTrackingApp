package com.example.collectorstrackingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.collectorstrackingapp.data.AppDatabase
import com.example.collectorstrackingapp.data.CollectibleRepository
import com.example.collectorstrackingapp.data.SyncEngine
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible

/**
 * Provides the collection to the screens. Reads come straight from the phone's database;
 * writes go through [SyncEngine], which saves them here and then in the cloud.
 */
class CollectibleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CollectibleRepository
    private val sync = SyncEngine.get(application)
    val allCollectibles: LiveData<List<Collectible>>
    val allCategories: LiveData<List<Category>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CollectibleRepository(database.collectibleDao(), database.categoryDao())
        allCollectibles = repository.allCollectibles.asLiveData()
        allCategories = repository.allCategories.asLiveData()

        // Every screen gets its own ViewModel; the engine pulls only once per signed-in account.
        sync.pullIfNeeded()
    }

    fun getCollectible(id: Int): LiveData<Collectible> {
        return repository.getCollectible(id).asLiveData()
    }

    fun searchCollectibles(query: String?, category: String?): LiveData<List<Collectible>> {
        return repository.searchCollectibles(query, category).asLiveData()
    }

    fun insert(collectible: Collectible) = sync.save(collectible)

    fun delete(collectible: Collectible) = sync.delete(collectible)

    fun insertCategory(category: Category) = sync.saveCategory(category)

    fun deleteCategory(category: Category) = sync.deleteCategory(category)
}
