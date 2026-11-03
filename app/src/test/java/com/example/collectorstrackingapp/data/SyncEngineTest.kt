package com.example.collectorstrackingapp.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sync rules, against a real in-memory database and a fake cloud. Two engines sharing one
 * [FakeCloud] stand in for two phones signed into the same account.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class SyncEngineTest {

    private val cloud = FakeCloud()
    private val phones = mutableListOf<Phone>()

    private inner class Phone {
        val db: AppDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = CollectibleRepository(db.collectibleDao(), db.categoryDao())
        val engine = SyncEngine(repository, cloud, CoroutineScope(Dispatchers.Unconfined), FakePrefs(), { db.clearAllTables() })

        fun items() = repository.getAllNow()
        fun names() = items().map { it.name }.sorted()
        fun add(name: String) = runBlocking { engine.save(item(name)).join() }
        fun pull() = runBlocking { engine.pull() }
    }

    private fun phone() = Phone().also { phones += it }

    private fun item(name: String) = Collectible(name = name, category = "FunkoPop", description = "", timestamp = 1)

    @Before
    fun signIn() {
        cloud.uid = "ada"
    }

    @After
    fun closeDatabases() {
        phones.forEach { it.db.close() }
    }

    // --- bug 1: items from two phones used to share ids and overwrite each other ---

    @Test
    fun itemsAddedOnTwoPhonesBothSurvive() {
        val a = phone()
        val b = phone()

        a.add("Batman")
        b.add("Robin") // both phones' first item: local row 1 on each

        a.pull()
        b.pull()

        assertEquals(listOf("Batman", "Robin"), cloud.names())
        assertEquals(listOf("Batman", "Robin"), a.names())
        assertEquals(listOf("Batman", "Robin"), b.names())
        assertNotEquals(a.items()[0].cloudId, a.items()[1].cloudId)
    }

    @Test
    fun anItemIsMarkedSyncedOnceTheCloudHasIt() {
        val a = phone()
        a.add("Batman")
        assertTrue(a.items().single().synced)

        cloud.offline = true
        a.add("Robin")
        assertFalse(a.items().single { it.name == "Robin" }.synced)
    }

    @Test
    fun editingUpdatesTheCloudCopyInsteadOfAddingOne() {
        val a = phone()
        a.add("Batman")
        val saved = a.items().single()

        runBlocking { a.engine.save(saved.copy(name = "Batman (1989)")).join() }

        assertEquals(listOf("Batman (1989)"), cloud.names())
    }

    // --- bug 3: deletes didn't reach other phones ---

    @Test
    fun anItemDeletedOnOnePhoneIsDeletedOnTheOther() {
        val a = phone()
        val b = phone()
        a.add("Batman")
        b.pull()
        assertEquals(listOf("Batman"), b.names())

        runBlocking { a.engine.delete(a.items().single()).join() }
        b.pull()

        assertTrue(b.items().isEmpty())
    }

    @Test
    fun anItemThatNeverReachedTheCloudIsUploadedNotDeleted() {
        val a = phone()
        cloud.offline = true
        a.add("Robin")
        cloud.offline = false

        a.pull()

        assertEquals(listOf("Robin"), a.names())
        assertEquals(listOf("Robin"), cloud.names())
        assertTrue(a.items().single().synced)
    }

    @Test
    fun anOfflinePullChangesNothing() {
        val a = phone()
        a.add("Batman")
        cloud.offline = true

        assertFalse(a.pull())
        assertEquals(listOf("Batman"), a.names())
    }

    @Test
    fun unsyncedLocalChangesWinOverTheCloudCopy() {
        val a = phone()
        a.add("Batman")
        cloud.offline = true
        runBlocking { a.engine.save(a.items().single().copy(name = "Batman (1989)")).join() }
        cloud.offline = false

        a.pull()

        assertEquals(listOf("Batman (1989)"), a.names())
        assertEquals(listOf("Batman (1989)"), cloud.names())
    }

    // --- bug 2: a second account's collection didn't load until the app restarted ---

    @Test
    fun aSecondAccountGetsItsOwnPull() = runBlocking {
        val a = phone()
        cloud.uid = "ada"
        a.engine.pullIfNeeded()?.join()
        assertNull("same account, no second pull", a.engine.pullIfNeeded())

        a.engine.signedOut().join()
        cloud.uid = "grace"

        assertTrue("new account pulls", a.engine.pullIfNeeded() != null)
    }

    @Test
    fun signingBackInPullsTheCollectionAgain() = runBlocking {
        val a = phone()
        a.add("Batman")
        a.engine.pullIfNeeded()?.join()

        a.engine.signedOut().join()
        assertTrue("signing out empties the phone", a.items().isEmpty())

        a.engine.pullIfNeeded()?.join()
        assertEquals(listOf("Batman"), a.names())
    }

    @Test
    fun aFailedPullIsRetriedByTheNextScreen() = runBlocking {
        val a = phone()
        cloud.offline = true
        a.engine.pullIfNeeded()?.join()
        cloud.offline = false

        assertTrue(a.engine.pullIfNeeded() != null)
    }

    // --- photos ---

    @Test
    fun aLocalPhotoIsUploadedAndTheItemPointsAtTheCloudCopy() {
        val a = phone()
        runBlocking { a.engine.save(item("Batman").copy(imagePath = "/data/photos/batman.jpg")).join() }

        assertEquals("firestore-photo://batman", a.items().single().imagePath)
        assertEquals("firestore-photo://batman", cloud.items.values.single().imagePath)
        assertEquals(setOf("firestore-photo://batman"), cloud.photos)
    }

    @Test
    fun deletingAnItemDeletesItsPhotoFromTheCloud() {
        val a = phone()
        runBlocking { a.engine.save(item("Batman").copy(imagePath = "/data/photos/batman.jpg")).join() }

        runBlocking { a.engine.delete(a.items().single()).join() }

        assertTrue(cloud.photos.isEmpty())
    }

    @Test
    fun replacingAPhotoDeletesTheOldOneFromTheCloud() {
        val a = phone()
        runBlocking { a.engine.save(item("Batman").copy(imagePath = "/data/photos/front.jpg")).join() }

        runBlocking { a.engine.save(a.items().single().copy(imagePath = "/data/photos/back.jpg")).join() }

        assertEquals(setOf("firestore-photo://back"), cloud.photos)
        assertEquals("firestore-photo://back", a.items().single().imagePath)
    }

    @Test
    fun editingAnItemKeepsItsPhoto() {
        val a = phone()
        runBlocking { a.engine.save(item("Batman").copy(imagePath = "/data/photos/front.jpg")).join() }

        runBlocking { a.engine.save(a.items().single().copy(name = "Batman (1989)")).join() }

        assertEquals(setOf("firestore-photo://front"), cloud.photos)
    }

    @Test
    fun aPhotoThatCouldntUploadStaysOnThePhoneAndGoesUpOnTheNextSync() {
        val a = phone()
        cloud.offline = true
        runBlocking { a.engine.save(item("Batman").copy(imagePath = "/data/photos/batman.jpg")).join() }
        assertEquals("/data/photos/batman.jpg", a.items().single().imagePath)
        cloud.offline = false

        a.pull()

        assertEquals("firestore-photo://batman", a.items().single().imagePath)
        assertEquals("firestore-photo://batman", cloud.items.values.single().imagePath)
    }

    @Test
    fun categoriesComeDownOnPull() {
        val a = phone()
        cloud.categories += "Comics"

        a.pull()

        val categories = runBlocking { a.repository.allCategories.first() }
        assertTrue(categories.any { it.name == "Comics" })
    }

    private class FakePrefs : SyncPrefs {
        private val repaired = mutableSetOf<String>()
        override fun isRepaired(uid: String) = uid in repaired
        override fun markRepaired(uid: String) {
            repaired += uid
        }
    }

    /** One account's cloud copy, shared by every phone in a test. */
    private class FakeCloud : CloudSync {
        var uid: String? = null
        var offline = false
        val items = linkedMapOf<String, Collectible>()
        val categories = mutableSetOf<String>()

        fun names() = items.values.map { it.name }.sorted()

        override val currentUserId get() = uid

        override suspend fun fetchCollectibles() =
            if (offline || uid == null) null else items.values.map { it.copy(id = 0, synced = true) }

        override suspend fun fetchCategories() =
            if (offline || uid == null) null else categories.map { Category(name = it) }

        override suspend fun upsertCollectible(collectible: Collectible): Boolean {
            if (offline) return false
            items[collectible.cloudId] = collectible.copy(id = 0)
            return true
        }

        override suspend fun deleteCollectible(cloudId: String): Boolean {
            if (offline) return false
            items.remove(cloudId)
            return true
        }

        override suspend fun upsertCategory(category: Category): Boolean {
            if (offline) return false
            categories += category.name
            return true
        }

        override suspend fun deleteCategory(category: Category): Boolean {
            if (offline) return false
            categories -= category.name
            return true
        }

        val photos = mutableSetOf<String>()

        override suspend fun uploadImage(localPath: String): String? {
            if (offline) return null
            val path = CloudPhotos.pathFor(localPath.substringAfterLast('/').substringBefore('.'))
            photos += path
            return path
        }

        override suspend fun deletePhoto(path: String): Boolean {
            if (offline) return false
            photos -= path
            return true
        }

        override suspend fun deleteEverything(): Boolean {
            if (offline) return false
            items.clear()
            categories.clear()
            return true
        }
    }
}
