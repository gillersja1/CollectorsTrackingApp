package com.example.collectorstrackingapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible

/**
 * The Room database for this app.
 *
 * Every version's schema is exported to app/schemas and committed. A schema change needs a new
 * version and a [Migration] here, plus a test in MigrationTest: there's no destructive fallback
 * from version 7 on, so an update can never wipe items that haven't reached the cloud yet.
 */
@Database(entities = [Collectible::class, Category::class], version = 8, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectibleDao(): CollectibleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 7 to 8: each item gets a cloud id and a synced flag, and the unused users table goes
         * (sign-in has been Firebase's job for a long time). Existing items take their row number
         * as their cloud id, because that's the document id older builds uploaded them under;
         * they start unsynced, so the next sync uploads them again under that id.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collectibles ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE collectibles SET cloudId = CAST(id AS TEXT)")
                db.execSQL("ALTER TABLE collectibles ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_collectibles_cloudId ON collectibles (cloudId)")
                db.execSQL("DROP TABLE IF EXISTS users")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_7_8)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "collectible_database"
                )
                .addMigrations(*MIGRATIONS)
                // Versions before 7 predate exported schemas and have no migrations.
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
                .addCallback(AppDatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                try {
                    db.execSQL("INSERT INTO categories (name) VALUES ('FunkoPop')")
                } catch (e: Exception) {
                    android.util.Log.e("CollectorsApp", "Unexpected error", e)
                }
            }
        }
    }
}
