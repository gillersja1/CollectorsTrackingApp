package com.example.collectorstrackingapp

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.collectorstrackingapp.data.CloudPhotoFetcher
import com.example.collectorstrackingapp.data.FirebaseSyncManager
import com.google.firebase.FirebaseApp

class CollectorsApp : Application(), ImageLoaderFactory {
    companion object {
        private const val TAG = "CollectorsApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CollectorsApp onCreate started")

        // Log the crash, then hand it on: the system's own handler ends the process and records the
        // crash. Swallowing it here left a dead app running and hid crashes from Android's reports.
        val systemHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            systemHandler?.uncaughtException(thread, throwable)
        }

        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "FirebaseApp initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseApp initialization failed", e)
        }
    }

    /** Coil's image loader, taught to show photos stored in Firestore (`firestore-photo://...`). */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(CloudPhotoFetcher.Factory(this@CollectorsApp, FirebaseSyncManager(this@CollectorsApp))) }
        .build()
}
