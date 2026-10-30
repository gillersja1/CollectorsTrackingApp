package com.example.collectorstrackingapp.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.collectorstrackingapp.model.Collectible
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Adding and deleting collectibles end to end against a real in-memory Room
 * database, following the same calls the Add/Edit screen makes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class CollectibleAddDeleteTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CollectibleRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CollectibleRepository(db.collectibleDao(), db.categoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** A new item as the Add screen builds it: no id, so Room assigns one. */
    private fun newItem(name: String, category: String = "MTG", timestamp: Long = 1_000) =
        Collectible(name = name, category = category, description = "", timestamp = timestamp)

    private fun names() = runBlocking { repository.allCollectibles.first().map { it.name } }

    // --- adding ---

    @Test
    fun add_savesEveryField() = runBlocking {
        val item = Collectible(
            name = "Black Lotus",
            category = "MTG",
            description = "Alpha edition",
            imagePath = "/data/images/lotus.jpg",
            timestamp = 1_234,
            isWishlist = true,
            estimatedValue = 25_000.50,
            tags = "power nine,alpha"
        )

        val id = repository.insert(item)

        assertEquals(item.copy(id = id), repository.getCollectible(id).first())
    }

    @Test
    fun add_newItemsGetDistinctIds() {
        val first = repository.insert(newItem("Sol Ring"))
        val second = repository.insert(newItem("Mox Pearl"))

        assertTrue(first > 0)
        assertNotEquals(first, second)
    }

    @Test
    fun add_newItemAppearsInListAndSearch() {
        repository.insert(newItem("Sol Ring", category = "MTG"))

        assertEquals(listOf("Sol Ring"), names())
        assertEquals(
            listOf("Sol Ring"),
            runBlocking { repository.searchCollectibles("sol", "MTG").first().map { it.name } }
        )
    }

    @Test
    fun edit_withReturnedId_updatesInsteadOfDuplicating() = runBlocking {
        // Saving an edit reuses the id from the first save; it must not add a row.
        val id = repository.insert(newItem("Sol Ring"))

        repository.insert(newItem("Sol Ring (Foil)").copy(id = id))

        assertEquals(listOf("Sol Ring (Foil)"), names())
        assertEquals(id, repository.getAllNow().single().id)
    }

    @Test
    fun add_withIdZero_alwaysCreatesANewRow() {
        // id 0 means "not saved yet" to Room. This is why syncing items as id 0
        // turned every cloud re-import into a duplicate.
        repository.insert(newItem("Sol Ring"))
        repository.insert(newItem("Sol Ring"))

        assertEquals(2, repository.getAllNow().size)
    }

    // --- deleting ---

    @Test
    fun delete_removesTheItemFromListAndSearch() = runBlocking {
        val id = repository.insert(newItem("Sol Ring"))

        repository.delete(repository.getCollectible(id).first())

        assertTrue(names().isEmpty())
        assertTrue(repository.searchCollectibles("sol", null).first().isEmpty())
    }

    @Test
    fun delete_leavesOtherItemsAlone() = runBlocking {
        val keepId = repository.insert(newItem("Keep"))
        val removeId = repository.insert(newItem("Remove"))

        repository.delete(repository.getCollectible(removeId).first())

        assertEquals(listOf(keepId), repository.getAllNow().map { it.id })
    }

    @Test
    fun delete_matchesByIdOnly() {
        // The edit screen falls back to deleting a blank item with just the id
        // when the item hasn't finished loading.
        val id = repository.insert(newItem("Sol Ring"))

        repository.delete(Collectible(id = id, name = "", category = "", description = ""))

        assertTrue(repository.getAllNow().isEmpty())
    }

    @Test
    fun delete_missingItem_doesNothing() {
        repository.insert(newItem("Sol Ring"))

        repository.delete(Collectible(id = 9_999, name = "Ghost", category = "", description = ""))

        assertEquals(listOf("Sol Ring"), names())
    }

    @Test
    fun delete_isPermanent_untilTheItemIsAddedAgain() {
        val id = repository.insert(newItem("Sol Ring"))
        repository.delete(Collectible(id = id, name = "", category = "", description = ""))

        assertTrue(repository.getAllNow().isEmpty())

        repository.insert(newItem("Sol Ring"))
        assertEquals(listOf("Sol Ring"), names())
    }

    @Test
    fun delete_idIsNeverReusedByTheNextAdd() {
        // The cloud copy is keyed by id. If a new item reused a deleted item's id,
        // a late cloud delete or re-import could hit the wrong item.
        val deletedId = repository.insert(newItem("Sol Ring"))
        repository.delete(Collectible(id = deletedId, name = "", category = "", description = ""))

        val nextId = repository.insert(newItem("Mox Pearl"))

        assertTrue(nextId > deletedId)
    }

    @Test
    fun addThenDeleteEverything_leavesAnEmptyCollection() = runBlocking {
        val ids = listOf("A", "B", "C").map { repository.insert(newItem(it)) }

        ids.forEach { repository.delete(repository.getCollectible(it).first()) }

        assertTrue(repository.allCollectibles.first().isEmpty())
    }
}
