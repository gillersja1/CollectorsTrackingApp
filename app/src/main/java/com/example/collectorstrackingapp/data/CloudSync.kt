package com.example.collectorstrackingapp.data

import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible

/**
 * The cloud copy of the signed-in user's collection. [FirebaseSyncManager] is the real one;
 * tests use a fake, so the sync rules in [SyncEngine] can be checked without Firebase.
 */
interface CloudSync {
    /** The signed-in account, or null when nobody is signed in. */
    val currentUserId: String?

    /** Every item in the cloud, or null if the cloud couldn't be read (offline, signed out). */
    suspend fun fetchCollectibles(): List<Collectible>?

    suspend fun fetchCategories(): List<Category>?

    /** Saves [collectible] under its cloud id. True once the cloud has confirmed it. */
    suspend fun upsertCollectible(collectible: Collectible): Boolean

    suspend fun deleteCollectible(cloudId: String): Boolean

    suspend fun upsertCategory(category: Category): Boolean

    suspend fun deleteCategory(category: Category): Boolean

    /** Uploads a photo stored on this phone; returns the path to its cloud copy, or null if it couldn't. */
    suspend fun uploadImage(localPath: String): String?

    /** Deletes a photo uploaded by [uploadImage]; true for anything that isn't a cloud photo. */
    suspend fun deletePhoto(path: String): Boolean

    /** Deletes the whole account's cloud data: every item, category and photo. True only if all of it went. */
    suspend fun deleteEverything(): Boolean
}
