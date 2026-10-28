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

    private val auth: FirebaseAuth?
        get() = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }

    private val db: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }

    private val storage: FirebaseStorage?
        get() = try { FirebaseStorage.getInstance() } catch (_: Exception) { null }

    private val userId: String?
        get() = try { auth?.currentUser?.uid } catch (_: Exception) { null }

    private val collectiblesRef
        get() = try {
            userId?.let { uid -> db?.collection("users")?.document(uid)?.collection("collectibles") }
        } catch (_: Exception) { null }

    private val categoriesRef
        get() = try {
            userId?.let { uid -> db?.collection("users")?.document(uid)?.collection("categories") }
        } catch (_: Exception) { null }

    suspend fun uploadImageAndGetUrl(imageUriString: String): String? {
        return try {
            val uid = userId ?: return null
            val st = storage ?: return null
            if (!imageUriString.startsWith("content://") && !imageUriString.startsWith("file://")) {
                return imageUriString
            }
            val uri = Uri.parse(imageUriString)
            val fileName = "collectibles_${UUID.randomUUID()}.jpg"
            val ref = st.reference.child("users/$uid/images/$fileName")

            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun syncCollectibleInsert(collectible: Collectible) {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncCollectibleDelete(collectible: Collectible) {
        try {
            val ref = collectiblesRef ?: return
            ref.document(collectible.id.toString()).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncCategoryInsert(category: Category) {
        try {
            val ref = categoriesRef ?: return
            val data = mapOf(
                "id" to category.id,
                "name" to category.name
            )
            ref.document(category.name).set(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncCategoryDelete(category: Category) {
        try {
            val ref = categoriesRef ?: return
            ref.document(category.name).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun listenToCollectibles(onCollectiblesChanged: (List<Collectible>) -> Unit) {
        try {
            val ref = collectiblesRef ?: return
            ref.addSnapshotListener { snapshot, e ->
                try {
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
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
                            } catch (ex: Exception) {
                                null
                            }
                        }
                        onCollectiblesChanged(list)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun listenToCategories(onCategoriesChanged: (List<Category>) -> Unit) {
        try {
            val ref = categoriesRef ?: return
            ref.addSnapshotListener { snapshot, e ->
                try {
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getLong("id")?.toInt() ?: 0
                                val name = doc.getString("name") ?: ""
                                Category(id = id, name = name)
                            } catch (ex: Exception) {
                                null
                            }
                        }
                        onCategoriesChanged(list)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
