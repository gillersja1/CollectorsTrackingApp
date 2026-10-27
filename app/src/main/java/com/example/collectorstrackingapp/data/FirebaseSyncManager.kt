package com.example.collectorstrackingapp.data

import android.content.Context
import android.net.Uri
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Manages synchronization of collectibles and categories with Cloud Firestore
 * and image uploads with Firebase Storage.
 */
class FirebaseSyncManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val userId: String?
        get() = auth.currentUser?.uid

    private val collectiblesRef
        get() = userId?.let { db.collection("users").document(it).collection("collectibles") }

    private val categoriesRef
        get() = userId?.let { db.collection("users").document(it).collection("categories") }

    suspend fun uploadImageAndGetUrl(imageUriString: String): String? {
        val uid = userId ?: return null
        if (!imageUriString.startsWith("content://") && !imageUriString.startsWith("file://")) {
            return imageUriString
        }
        try {
            val uri = Uri.parse(imageUriString)
            val fileName = "collectibles_${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("users/$uid/images/$fileName")

            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            return downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun syncCollectibleInsert(collectible: Collectible) {
        val ref = collectiblesRef ?: return
        val docId = collectible.id.toString()
        val data = mapOf(
            "id" to collectible.id,
            "name" to collectible.name,
            "category" to collectible.category,
            "description" to collectible.description,
            "imagePath" to (collectible.imagePath ?: ""),
            "timestamp" to collectible.timestamp,
            "isWishlist" to collectible.isWishlist,
            "estimatedValue" to collectible.estimatedValue,
            "tags" to collectible.tags
        )
        ref.document(docId).set(data)
    }

    fun syncCollectibleDelete(collectible: Collectible) {
        val ref = collectiblesRef ?: return
        ref.document(collectible.id.toString()).delete()
    }

    fun syncCategoryInsert(category: Category) {
        val ref = categoriesRef ?: return
        val data = mapOf(
            "id" to category.id,
            "name" to category.name
        )
        ref.document(category.name).set(data)
    }

    fun syncCategoryDelete(category: Category) {
        val ref = categoriesRef ?: return
        ref.document(category.name).delete()
    }

    fun listenToCollectibles(onCollectiblesChanged: (List<Collectible>) -> Unit) {
        val ref = collectiblesRef ?: return
        ref.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id")?.toInt() ?: 0
                    val name = doc.getString("name") ?: ""
                    val category = doc.getString("category") ?: ""
                    val description = doc.getString("description") ?: ""
                    val imagePath = doc.getString("imagePath")
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val isWishlist = doc.getBoolean("isWishlist") ?: false
                    val estimatedValue = doc.getDouble("estimatedValue") ?: 0.0
                    val tags = doc.getString("tags") ?: ""
                    Collectible(id = id, name = name, category = category, description = description, imagePath = imagePath, timestamp = timestamp, isWishlist = isWishlist, estimatedValue = estimatedValue, tags = tags)
                }
                onCollectiblesChanged(list)
            }
        }
    }

    fun listenToCategories(onCategoriesChanged: (List<Category>) -> Unit) {
        val ref = categoriesRef ?: return
        ref.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id")?.toInt() ?: 0
                    val name = doc.getString("name") ?: ""
                    Category(id = id, name = name)
                }
                onCategoriesChanged(list)
            }
        }
    }
}
