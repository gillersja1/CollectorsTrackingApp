package com.example.collectorstrackingapp.data

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Upgrading the database keeps the collection. Each test builds an old version from its exported
 * schema (app/schemas), fills it, runs the real migrations, and checks Room accepts the result.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun version7To8KeepsEveryItemAndGivesItTheIdTheCloudAlreadyUses() {
        helper.createDatabase(dbName, 7).apply {
            execSQL(
                "INSERT INTO collectibles (id, name, category, description, imagePath, timestamp, isWishlist, estimatedValue, tags) " +
                    "VALUES (5, 'Batman', 'FunkoPop', 'First edition', '/photos/b.jpg', 100, 0, 12.5, 'dc,hero')"
            )
            execSQL(
                "INSERT INTO collectibles (id, name, category, description, imagePath, timestamp, isWishlist, estimatedValue, tags) " +
                    "VALUES (9, 'Robin', 'FunkoPop', '', NULL, 200, 1, 0.0, '')"
            )
            execSQL("INSERT INTO categories (name) VALUES ('FunkoPop')")
            execSQL("INSERT INTO users (username, passwordHash) VALUES ('old@example.com', 'x')")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 8, true, *AppDatabase.MIGRATIONS).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val items = db.collectibleDao().getAllNow()
            assertEquals(listOf("Batman", "Robin"), items.map { it.name })
            val batman = items[0]
            assertEquals("5", batman.cloudId)
            assertEquals("First edition", batman.description)
            assertEquals("/photos/b.jpg", batman.imagePath)
            assertEquals(12.5, batman.estimatedValue, 0.0)
            assertEquals("dc,hero", batman.tags)
            assertEquals("9", items[1].cloudId)
            assertEquals(true, items[1].isWishlist)
            // The cloud may have older copies: start unsynced, so the next sync sends these.
            assertFalse(items.any { it.synced })
            assertEquals(listOf("FunkoPop"), runBlocking { db.categoryDao().getAllCategories().first().map { it.name } })
        } finally {
            db.close()
        }
    }

    @Test
    fun theUsersTableIsGoneAfterTheMigration() {
        helper.createDatabase(dbName, 7).close()

        val db = helper.runMigrationsAndValidate(dbName, 8, true, *AppDatabase.MIGRATIONS)
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'users'").use { cursor ->
            assertEquals(0, cursor.count)
        }
        db.close()
    }
}
