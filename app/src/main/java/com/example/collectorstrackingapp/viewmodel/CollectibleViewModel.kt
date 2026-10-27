package com.example.collectorstrackingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.collectorstrackingapp.data.AppDatabase
import com.example.collectorstrackingapp.data.CollectibleRepository
import com.example.collectorstrackingapp.data.FirebaseSyncManager
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel to provide data to the UI and survive configuration changes.
 */
class CollectibleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CollectibleRepository
    private val syncManager = FirebaseSyncManager(application)
    val allCollectibles: LiveData<List<Collectible>>
    val allCategories: LiveData<List<Category>>

    init {
        val database = AppDatabase.getDatabase(application)
        val collectibleDao = database.collectibleDao()
        val categoryDao = database.categoryDao()
        repository = CollectibleRepository(collectibleDao, categoryDao)
        allCollectibles = repository.allCollectibles.asLiveData()
        allCategories = repository.allCategories.asLiveData()

        // Listen to remote changes in real-time using addSnapshotListener
        syncManager.listenToCollectibles { remoteList ->
            viewModelScope.launch(Dispatchers.IO) {
                for (item in remoteList) {
                    repository.insert(item)
                }
            }
        }
        syncManager.listenToCategories { remoteCategories ->
            viewModelScope.launch(Dispatchers.IO) {
                for (cat in remoteCategories) {
                    repository.insertCategory(cat)
                }
            }
        }
    }

    fun getCollectible(id: Int): LiveData<Collectible> {
        return repository.getCollectible(id).asLiveData()
    }

    fun searchCollectibles(query: String?, category: String?): LiveData<List<Collectible>> {
        return repository.searchCollectibles(query, category).asLiveData()
    }

    fun insert(collectible: Collectible) = viewModelScope.launch(Dispatchers.IO) {
        var finalCollectible = collectible
        if (!collectible.imagePath.isNullOrEmpty() && (collectible.imagePath.startsWith("content://") || collectible.imagePath.startsWith("file://"))) {
            val cloudUrl = syncManager.uploadImageAndGetUrl(collectible.imagePath)
            if (cloudUrl != null) {
                finalCollectible = collectible.copy(imagePath = cloudUrl)
            }
        }
        repository.insert(finalCollectible)
        syncManager.syncCollectibleInsert(finalCollectible)
    }

    fun delete(collectible: Collectible) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(collectible)
        syncManager.syncCollectibleDelete(collectible)
    }

    fun insertCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertCategory(category)
        syncManager.syncCategoryInsert(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteCategory(category)
        syncManager.syncCategoryDelete(category)
    }
}
