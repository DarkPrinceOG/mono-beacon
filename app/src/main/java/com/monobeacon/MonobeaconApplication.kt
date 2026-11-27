package com.monobeacon

import android.app.Application
import androidx.multiprocessing.gradle.MLQuickStep

/**
 * MONO // BEACON Application Class
 * - Safe Firebase initialization (guarded if google-services.json missing)
 * - DataStore defaults initialization
 */
class MonobeaconApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Firebase initialization is safely handled in components
        // that check FirebaseApp.getApps(context).isNotEmpty() before use

        // Initialize DataStore defaults
        // This will be called on first preference access
    }
}
