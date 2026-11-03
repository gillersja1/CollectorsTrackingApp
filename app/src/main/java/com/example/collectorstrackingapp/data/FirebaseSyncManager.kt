package com.example.collectorstrackingapp.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * The signed-in user's collection in Cloud Firestore (users/{uid}/collectibles and
 * users/{uid}/categories) and their photos in Firebase Storage (users/{uid}/images).
 * Failures are logged under the "CloudSync" tag and reported as false/null, never thrown.
 */
class FirebaseSyncManager(private val context: Context) : CloudSync {

    private val ready: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    override val currentUserId: String?
        get() = if (ready) FirebaseAuth.getInstance().currentUser?.uid else null

    private fun userCollection(name: String): CollectionReference? {
        val uid = currentUserId ?: return null
        return FirebaseFirestore.getInstance().collection("users").document(uid).collection(name)
    }

    /** Runs [block], logging and swallowing anything except coroutine cancellation. */
    private suspend fun <T> attempt(what: String, fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$what failed", e)
            fallback
        }

    override suspend fun fetchCollectibles(): List<Collectible>? = attempt("fetch collectibles", null) {
        val ref = userCollection(COLLECTIBLES) ?: return@attempt null
        ref.get().await().documents
            // Older builds uploaded some new items as document "0"; that copy is never real.
            .filter { it.id != "0" }
            .mapNotNull(::toCollectible)
    }

    override suspend fun fetchCategories(): List<Category>? = attempt("fetch categories", null) {
        val ref = userCollection(CATEGORIES) ?: return@attempt null
        ref.get().await().documents.mapNotNull { doc ->
            doc.getString("name")?.takeIf { it.isNotBlank() }?.let { Category(name = it) }
        }
    }

    override suspend fun upsertCollectible(collectible: Collectible): Boolean =
        attempt("save ${collectible.cloudId}", false) {
            val ref = userCollection(COLLECTIBLES) ?: return@attempt false
            val data = mapOf(
                "cloudId" to collectible.cloudId,
                "name" to collectible.name,
                "category" to collectible.category,
                "description" to collectible.description,
                "imagePath" to (collectible.imagePath ?: ""),
                "timestamp" to collectible.timestamp,
                "isWishlist" to collectible.isWishlist,
                "estimatedValue" to collectible.estimatedValue,
                "tags" to collectible.tags
            )
            ref.document(collectible.cloudId).set(data).await()
            true
        }

    override suspend fun deleteCollectible(cloudId: String): Boolean = attempt("delete $cloudId", false) {
        val ref = userCollection(COLLECTIBLES) ?: return@attempt false
        ref.document(cloudId).delete().await()
        true
    }

    override suspend fun upsertCategory(category: Category): Boolean =
        attempt("save category ${category.name}", false) {
            val ref = userCollection(CATEGORIES) ?: return@attempt false
            ref.document(category.name).set(mapOf("id" to category.id, "name" to category.name)).await()
            true
        }

    override suspend fun deleteCategory(category: Category): Boolean =
        attempt("delete category ${category.name}", false) {
            val ref = userCollection(CATEGORIES) ?: return@attempt false
            ref.document(category.name).delete().await()
            true
        }

    override suspend fun uploadImage(localPath: String): String? = attempt("upload $localPath", null) {
        val uid = currentUserId ?: return@attempt null
        if (!ImagePaths.isLocal(localPath)) return@attempt null
        // The item screen copies picked photos into app storage and saves the plain path.
        val uri = if (localPath.startsWith("/")) {
            val file = File(localPath)
            if (!file.exists()) return@attempt null
            Uri.fromFile(file)
        } else {
            Uri.parse(localPath)
        }
        val ref = FirebaseStorage.getInstance().reference
            .child("users/$uid/images/collectibles_${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    override suspend fun deleteEverything(): Boolean = attempt("delete the account's cloud data", false) {
        val uid = currentUserId ?: return@attempt false
        for (name in listOf(COLLECTIBLES, CATEGORIES)) {
            val docs = userCollection(name)!!.get().await().documents
            // A batch holds at most 500 writes.
            for (chunk in docs.chunked(450)) {
                val batch = FirebaseFirestore.getInstance().batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).delete().await()
        val images = FirebaseStorage.getInstance().reference.child("users/$uid/images").listAll().await()
        images.items.forEach { it.delete().await() }
        true
    }

    private fun toCollectible(doc: DocumentSnapshot): Collectible? = try {
        Collectible(
            // Items from older builds have no cloudId field; their document id is it.
            cloudId = doc.getString("cloudId")?.takeIf { it.isNotBlank() } ?: doc.id,
            name = doc.getString("name") ?: "",
            category = doc.getString("category") ?: "",
            description = doc.getString("description") ?: "",
            imagePath = doc.getString("imagePath")?.takeIf { it.isNotBlank() },
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
            isWishlist = doc.getBoolean("isWishlist") ?: false,
            estimatedValue = doc.getDouble("estimatedValue") ?: 0.0,
            tags = doc.getString("tags") ?: "",
            synced = true
        )
    } catch (e: Exception) {
        Log.w(TAG, "skipping unreadable document ${doc.id}", e)
        null
    }

    private companion object {
        const val TAG = "CloudSync"
        const val COLLECTIBLES = "collectibles"
        const val CATEGORIES = "categories"
    }
}
