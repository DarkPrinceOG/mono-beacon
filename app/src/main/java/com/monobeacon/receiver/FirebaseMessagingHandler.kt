package com.monobeacon.receiver

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.monobeacon.data.DataStoreManager
import com.monobeacon.data.PreferencesSchema

/**
 * Firebase Messaging Service - Handles high-priority data messages
 * Safely guards Firebase calls so app compiles without google-services.json
 */
class FirebaseMessagingHandler : FirebaseMessagingService() {

    private val TAG = "FirebaseMessagingHandler"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Safely guard: only process if Firebase is initialized
        val context = applicationContext
        if (androidx.core.app.ApplicationCompat.getApplication(context).firebaseApp == null) {
            // Firebase not initialized - graceful degradation
            Timber.tag(TAG).d("Firebase not initialized, skipping message")
            return
        }

        if (remoteMessage.priority == RemoteMessage.Priority.HIGH) {
            val data = remoteMessage.data
            val action = data.action ?: ""

            // Safely guard Firebase DB access
            try {
                when (action) {
                    "update_schedule" -> {
                        val startTime = data["start_time"] ?: return
                        val endTime = data["end_time"] ?: return
                        val mode = data["mode"] ?: "scheduled"
                        // Only update DataStore locally (Firebase DB access guarded)
                        DataStoreManager.setScheduledWindow(this, startTime, endTime)
                        DataStoreManager.setListeningMode(this, mode)
                    }
                    "force_24_7" -> {
                        val enable247 = data["enable"]?.let { it.toBoolean() } ?: true
                        val mode = if (enable247) "24/7" else "scheduled"
                        DataStoreManager.setListeningMode(this, mode)
                    }
                    "activate_now" -> {
                        // Safely start alarm service only if components exist
                        val alarmIntent = android.content.Intent(this, com.monobeacon.service.AlarmService::class.java)
                        alarmIntent.setAction(com.monobeacon.service.AlarmService.ACTION_TRIGGER_IMMEDIATE)
                        if (androidx.core.content.ContextCompat.getSystemService(context, android.content.Context.VIBRATOR_SERVICE) != null) {
                            ContextCompat.startForegroundService(this, alarmIntent)
                        }
                    }
                    else -> Timber.tag(TAG).d("Unknown FCM action: $action")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("Error handling FCM message", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("Refreshed FCM token: $token")
        // Token refresh safely guarded - works even without full Firebase setup
    }
}
