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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ViewModel to provide data to the UI and survive configuration changes.
 */
class CollectibleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CollectibleRepository
    private val syncManager by lazy { FirebaseSyncManager(application) }
    val allCollectibles: LiveData<List<Collectible>>
    val allCategories: LiveData<List<Category>>

    init {
        val database = AppDatabase.getDatabase(application)
        val collectibleDao = database.collectibleDao()
        val categoryDao = database.categoryDao()
        repository = CollectibleRepository(collectibleDao, categoryDao)
        allCollectibles = repository.allCollectibles.asLiveData()
        allCategories = repository.allCategories.asLiveData()

        // Pull remote data once per app launch. Every screen gets its own ViewModel, so
        // without this guard each screen re-downloaded everything, and a download that
        // finished after a delete could put the deleted item back.
        if (remoteFetchStarted.compareAndSet(false, true)) viewModelScope.launch(Dispatchers.IO) {
            try {
                syncManager.fetchRemoteCollectibles { remoteList ->
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            // id 0 means "not saved yet" to Room, so inserting it creates a brand-new
                            // row on every sync. Older builds uploaded new items under id 0, so skip those.
                            for (item in remoteList.filter { it.id > 0 }) {
                                repository.insert(item)
                            }
                        } catch (_: Exception) {}
                    }
                }
                syncManager.fetchRemoteCategories { remoteCategories ->
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            for (cat in remoteCategories) {
                                repository.insertCategory(cat)
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
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
        try {
            if (!collectible.imagePath.isNullOrEmpty() && (collectible.imagePath.startsWith("content://") || collectible.imagePath.startsWith("file://"))) {
                val cloudUrl = syncManager.uploadImageAndGetUrl(collectible.imagePath)
                if (cloudUrl != null) {
                    finalCollectible = collectible.copy(imagePath = cloudUrl)
                }
            }
        } catch (_: Exception) {}
        // Save locally first so a new item gets its real id, then upload under that id.
        // Uploading first sent every new item to Firestore as id 0, so deletes never
        // matched the cloud copy and it was re-imported as a duplicate.
        val id = repository.insert(finalCollectible)
        try {
            syncManager.syncCollectibleInsert(finalCollectible.copy(id = id))
        } catch (_: Exception) {}
    }

    fun delete(collectible: Collectible) = viewModelScope.launch(Dispatchers.IO) {
        try {
            syncManager.syncCollectibleDelete(collectible)
        } catch (_: Exception) {}
        repository.delete(collectible)
    }

    fun insertCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        try {
            syncManager.syncCategoryInsert(category)
        } catch (_: Exception) {}
        repository.insertCategory(category)
    }

    companion object {
        private val remoteFetchStarted = AtomicBoolean(false)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        try {
            syncManager.syncCategoryDelete(category)
        } catch (_: Exception) {}
        repository.deleteCategory(category)
    }
}
