package com.example.collectorstrackingapp.data

import android.content.Context
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible

/**
 * Temporarily stubbed FirebaseSyncManager for debugging crash causes.
 */
class FirebaseSyncManager(private val context: Context) {

    suspend fun uploadImageAndGetUrl(imageUriString: String): String? {
        return imageUriString
    }

    fun syncCollectibleInsert(collectible: Collectible) {
        // No-op
    }

    fun syncCollectibleDelete(collectible: Collectible) {
        // No-op
    }

    fun syncCategoryInsert(category: Category) {
        // No-op
    }

    fun syncCategoryDelete(category: Category) {
        // No-op
    }

    fun listenToCollectibles(onCollectiblesChanged: (List<Collectible>) -> Unit) {
        // No-op
    }

    fun listenToCategories(onCategoriesChanged: (List<Category>) -> Unit) {
        // No-op
    }
}
