package com.example.collectorstrackingapp.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import com.example.collectorstrackingapp.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real Room queries against an in-memory SQLite database.
 * Robolectric runs them on the JVM, so no emulator is needed in CI.
 * Uses a plain Application so CollectorsApp's Firebase setup doesn't run.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class CollectibleRepositoryTest {

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

    private fun item(name: String, category: String = "FunkoPop", timestamp: Long = 0, tags: String = "") =
        Collectible(name = name, category = category, description = "", timestamp = timestamp, tags = tags)

    private fun search(query: String?, category: String? = null) =
        runBlocking { repository.searchCollectibles(query, category).first().map { it.name } }

    // --- collectibles ---

    @Test
    fun allCollectibles_areNewestFirst() = runBlocking {
        repository.insert(item("Old", timestamp = 100))
        repository.insert(item("New", timestamp = 300))
        repository.insert(item("Middle", timestamp = 200))

        assertEquals(listOf("New", "Middle", "Old"), repository.allCollectibles.first().map { it.name })
    }

    @Test
    fun insert_withExistingId_replacesTheItem() = runBlocking {
        repository.insert(item("Batman").copy(id = 7))
        repository.insert(item("Batman (Signed)").copy(id = 7))

        val all = repository.allCollectibles.first()
        assertEquals(1, all.size)
        assertEquals("Batman (Signed)", all.single().name)
    }

    @Test
    fun getCollectible_returnsItemById() = runBlocking {
        repository.insert(item("Charizard").copy(id = 42))

        assertEquals("Charizard", repository.getCollectible(42).first().name)
    }

    @Test
    fun delete_removesOnlyThatItem() = runBlocking {
        val keep = item("Keep").copy(id = 1)
        val remove = item("Remove").copy(id = 2)
        repository.insert(keep)
        repository.insert(remove)

        repository.delete(remove)

        assertEquals(listOf("Keep"), repository.allCollectibles.first().map { it.name })
    }

    // --- search ---

    @Test
    fun search_matchesPartialNameIgnoringCase() {
        repository.insert(item("Batman Funko"))
        repository.insert(item("Superman"))

        assertEquals(listOf("Batman Funko"), search("BAT"))
    }

    @Test
    fun search_matchesTags() {
        repository.insert(item("Grail", tags = "vintage,rare"))
        repository.insert(item("Common", tags = "modern"))

        assertEquals(listOf("Grail"), search("rare"))
    }

    @Test
    fun search_nullQueryAndAllCategory_returnsEverything() {
        repository.insert(item("A", "FunkoPop"))
        repository.insert(item("B", "Pokemon"))

        assertEquals(2, search(null, "All").size)
    }

    @Test
    fun search_combinesQueryAndCategory() {
        repository.insert(item("Pikachu Funko", "FunkoPop"))
        repository.insert(item("Pikachu Card", "Pokemon"))

        assertEquals(listOf("Pikachu Card"), search("pikachu", "Pokemon"))
    }

    @Test
    fun search_underscoreIsMatchedLiterally() {
        // Regression test: "_" is a LIKE wildcard, so before escaping it
        // matched every item with a name at least one character long.
        repository.insert(item("Batman"))
        repository.insert(item("Item_42"))

        assertEquals(listOf("Item_42"), search("_"))
    }

    @Test
    fun search_percentIsMatchedLiterally() {
        // Regression test: "%" previously matched everything.
        repository.insert(item("Batman"))
        repository.insert(item("100% Complete Set"))

        assertEquals(listOf("100% Complete Set"), search("%"))
    }

    @Test
    fun search_backslashIsMatchedLiterally() {
        repository.insert(item("AC\\DC Poster"))
        repository.insert(item("ACDC Shirt"))

        assertEquals(listOf("AC\\DC Poster"), search("C\\D"))
    }

    // --- categories ---

    @Test
    fun categories_areSortedByName() = runBlocking {
        repository.insertCategory(Category(name = "Pokemon"))
        repository.insertCategory(Category(name = "Comics"))
        repository.insertCategory(Category(name = "FunkoPop"))

        assertEquals(listOf("Comics", "FunkoPop", "Pokemon"), repository.allCategories.first().map { it.name })
    }

    @Test
    fun insertCategory_withExistingId_isIgnored() = runBlocking {
        repository.insertCategory(Category(id = 1, name = "FunkoPop"))
        repository.insertCategory(Category(id = 1, name = "Renamed"))

        assertEquals(listOf("FunkoPop"), repository.allCategories.first().map { it.name })
    }

    @Test
    fun insertCategory_withDuplicateName_isIgnored() = runBlocking {
        // Regression test: the unique index on name makes IGNORE apply to
        // duplicate names, not just duplicate IDs.
        repository.insertCategory(Category(name = "FunkoPop"))
        repository.insertCategory(Category(name = "FunkoPop"))

        assertEquals(listOf("FunkoPop"), repository.allCategories.first().map { it.name })
    }

    @Test
    fun deleteCategory_removesIt() = runBlocking {
        val comics = Category(id = 3, name = "Comics")
        repository.insertCategory(comics)

        repository.deleteCategory(comics)

        assertTrue(repository.allCategories.first().isEmpty())
    }

    // --- users ---

    @Test
    fun userLookup_isExactAndReturnsNullWhenMissing() {
        db.userDao().insertUser(User(username = "collector@example.com", passwordHash = "hash"))

        assertEquals("collector@example.com", db.userDao().getUserByUsername("collector@example.com")?.username)
        assertNull(db.userDao().getUserByUsername("someone@example.com"))
    }
}
