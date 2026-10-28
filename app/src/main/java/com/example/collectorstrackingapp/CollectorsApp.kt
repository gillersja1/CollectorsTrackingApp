package com.example.collectorstrackingapp

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Custom Application class to safely initialize Firebase and prevent startup crashes.
 */
class CollectorsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Catch any uncaught background/startup exceptions to prevent forced app closure
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
