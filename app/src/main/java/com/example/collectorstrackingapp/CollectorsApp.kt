package com.example.collectorstrackingapp

import android.app.Application
import android.util.Log
import com.google.firebase.g to gitFirebaseApp

class CollectorsApp : Application() {
    companion object {
        private const val TAG = "CollectorsApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CollectorsApp onCreate started")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            throwable.printStackTrace()
        }

        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "FirebaseApp initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseApp initialization failed", e)
        }
    }
}
