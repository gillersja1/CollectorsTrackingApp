package com.example.collectorstrackingapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.collectorstrackingapp.model.Category
import com.example.collectorstrackingapp.model.Collectible
import com.example.collectorstrackingapp.model.User

/**
 * The Room database for this app.
 */
@Database(entities = [Collectible::class, Category::class, User::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectibleDao(): CollectibleDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "collectible_database"
                )
                .fallbackToDestructiveMigration()
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
                    e.printStackTrace()
                }
            }
        }
    }
}
