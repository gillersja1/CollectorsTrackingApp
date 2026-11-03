package com.example.collectorstrackingapp

import android.app.Application
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings

/**
 * Runs the UI tests against the Firebase emulators, never the real project.
 *
 * It switches Firebase over right after the app starts and before any test touches it, which is the
 * only moment that works: Firebase refuses to change hosts once it has been used. From the Android
 * emulator, the computer running the Firebase emulators is 10.0.2.2; pass `-e firebaseHost <ip>` to
 * use another one.
 */
class FirebaseEmulatorRunner : AndroidJUnitRunner() {

    private var host = "10.0.2.2"

    override fun onCreate(arguments: Bundle?) {
        arguments?.getString("firebaseHost")?.let { host = it }
        super.onCreate(arguments)
    }

    override fun callApplicationOnCreate(app: Application) {
        super.callApplicationOnCreate(app)
        FirebaseAuth.getInstance().useEmulator(host, 9099)
        FirebaseFirestore.getInstance().apply {
            useEmulator(host, 8080)
            // No offline cache, so each test sees exactly what's in the emulator.
            firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings {}) }
        }
    }
}
