package com.monobeacon

import android.app.Application
import com.monobeacon.data.DataStoreManager
import com.google.firebase.FirebaseApp

/**
 * Main Application class - Initializes Firebase and DataStore defaults
 */
class MonobeaconApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize DataStore with defaults
        // This runs on app start to ensure preferences have default values
        // We use a coroutine here but in a real app, would use lifecycle-aware scope
        try {
            // Note: DataStore initialization is typically done in a ViewModel or
            // a dedicated initialization component. For simplicity, we set defaults
            // here that will be applied on first access.
            DataStoreManager.initializeDefaults(this).await()
        } catch (e: Exception) {
            // DataStore init failure is non-critical, defaults will be used
            android.util.Log.e("MonobeaconApp", "DataStore initialization error", e)
        }
    }
}
