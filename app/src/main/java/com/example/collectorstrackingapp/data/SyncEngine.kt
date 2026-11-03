package com.example.collectorstrackingapp.data

import android.content.Context
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps this phone's collection and the cloud copy in step.
 *
 * - Every write is saved on the phone first, then sent to the cloud; [Collectible.synced] turns
 *   true once the cloud confirms it.
 * - [pull] brings the phone up to date with the cloud: new and changed items come down, items
 *   another device deleted (synced here, gone from the cloud) are deleted here, and anything that
 *   never reached the cloud is sent up.
 * - Writes run in [scope], which outlives any screen, so leaving a screen can't cancel an upload.
 */
class SyncEngine(
    private val repository: CollectibleRepository,
    private val cloud: CloudSync,
    private val scope: CoroutineScope,
    private val prefs: SyncPrefs,
    private val clearLocal: suspend () -> Unit,
) {
    /** One pull, or sign-out cleanup, at a time. */
    private val lock = Mutex()

    @Volatile
    private var pulledFor: String? = null

    /**
     * Pulls once per signed-in account. Signing out resets it, so the next account, or the same
     * one signing back in, gets its collection.
     */
    fun pullIfNeeded(): Job? {
        val uid = cloud.currentUserId ?: return null
        synchronized(this) {
            if (pulledFor == uid) return null
            pulledFor = uid
        }
        return scope.launch {
            // Couldn't reach the cloud: let the next screen try again.
            if (!pull()) synchronized(this@SyncEngine) { if (pulledFor == uid) pulledFor = null }
        }
    }

    /** Signing out: forget the last pull and empty the phone's copy of the collection. */
    fun signedOut(): Job {
        synchronized(this) { pulledFor = null }
        return scope.launch { lock.withLock { clearLocal() } }
    }

    /** Returns false if the cloud couldn't be read; nothing on the phone changes then. */
    suspend fun pull(): Boolean = lock.withLock {
        val uid = cloud.currentUserId ?: return@withLock false
        val remote = cloud.fetchCollectibles() ?: return@withLock false
        repairOldSyncOnce(uid)

        val local = repository.getAllNow().associateBy { it.cloudId }
        val inCloud = remote.map { it.cloudId }.toSet()
        for (theirs in remote) {
            val mine = local[theirs.cloudId]
            when {
                mine == null -> repository.insert(theirs.copy(id = 0, synced = true))
                // Changes made on this phone that the cloud hasn't confirmed win; they're sent below.
                !mine.synced -> Unit
                else -> repository.insert(theirs.copy(id = mine.id, synced = true))
            }
        }
        for (mine in local.values) {
            when {
                // Synced before, gone from the cloud now: another device deleted it.
                mine.synced && mine.cloudId !in inCloud -> repository.delete(mine)
                !mine.synced -> push(mine)
            }
        }
        cloud.fetchCategories()?.forEach { repository.insertCategory(it) }
        uploadLocalImages()
        true
    }

    /** Saves [collectible] here, then in the cloud, uploading its photo first if it has a new one. */
    fun save(collectible: Collectible): Job = scope.launch {
        val id = repository.insert(collectible.copy(synced = false))
        val saved = collectible.copy(id = id, synced = false)
        push(withCloudPhoto(saved) ?: saved)
    }

    fun delete(collectible: Collectible): Job = scope.launch {
        repository.delete(collectible)
        cloud.deleteCollectible(collectible.cloudId)
    }

    fun saveCategory(category: Category): Job = scope.launch {
        repository.insertCategory(category)
        cloud.upsertCategory(category)
    }

    fun deleteCategory(category: Category): Job = scope.launch {
        repository.deleteCategory(category)
        cloud.deleteCategory(category)
    }

    private suspend fun push(collectible: Collectible) {
        if (cloud.upsertCollectible(collectible)) repository.markSyncedIfUnchanged(collectible)
    }

    /**
     * Uploads the item's photo if it's only on this phone and points the item at the cloud copy.
     * Returns the updated item, or null if there was nothing to do or the upload failed (the next
     * pull tries again).
     */
    private suspend fun withCloudPhoto(collectible: Collectible): Collectible? {
        val path = collectible.imagePath
        if (!ImagePaths.isLocal(path)) return null
        val url = cloud.uploadImage(path!!) ?: return null
        // Re-read, so an edit made during the upload isn't overwritten.
        val current = repository.getByCloudId(collectible.cloudId)?.takeIf { it.imagePath == path } ?: return null
        val updated = current.copy(imagePath = url, synced = false)
        repository.insert(updated)
        return updated
    }

    private suspend fun uploadLocalImages() {
        for (item in repository.getAllNow()) {
            withCloudPhoto(item)?.let { push(it) }
        }
    }

    /**
     * One-time cleanup, per account, for data written by an old sync bug that re-imported items
     * as copies and uploaded some new items as document "0".
     */
    private suspend fun repairOldSyncOnce(uid: String) {
        if (prefs.isRepaired(uid)) return
        for (duplicate in repository.removeDuplicates()) {
            cloud.deleteCollectible(duplicate.cloudId)
        }
        cloud.deleteCollectible("0")
        prefs.markRepaired(uid)
    }

    companion object {
        @Volatile
        private var instance: SyncEngine? = null

        /** The app's one engine; its scope lives as long as the app process. */
        fun get(context: Context): SyncEngine = instance ?: synchronized(this) {
            instance ?: run {
                val app = context.applicationContext
                val db = AppDatabase.getDatabase(app)
                SyncEngine(
                    repository = CollectibleRepository(db.collectibleDao(), db.categoryDao()),
                    cloud = FirebaseSyncManager(app),
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                    prefs = SharedSyncPrefs(app),
                    clearLocal = { db.clearAllTables() },
                ).also { instance = it }
            }
        }
    }
}

/** Remembers which accounts have had the one-time repair. */
interface SyncPrefs {
    fun isRepaired(uid: String): Boolean
    fun markRepaired(uid: String)
}

private class SharedSyncPrefs(context: Context) : SyncPrefs {
    private val prefs = context.getSharedPreferences("sync_repair", Context.MODE_PRIVATE)

    // Same key as before, so accounts already repaired aren't repaired again.
    override fun isRepaired(uid: String) = prefs.getBoolean("duplicate_fix_v1_$uid", false)

    override fun markRepaired(uid: String) {
        prefs.edit().putBoolean("duplicate_fix_v1_$uid", true).apply()
    }
}
