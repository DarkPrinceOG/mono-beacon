package com.monobeacon.receiver

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.monobeacon.data.DataStoreManager
import com.monobeacon.data.PreferencesSchema
import Timber.tag

/**
 * Firebase Messaging Service - Handles high-priority data messages from Firebase Cloud Messaging
 * Used for wake-up calls and remote schedule updates
 */
class FirebaseMessagingHandler : FirebaseMessagingService() {

    private val TAG = "FirebaseMessagingHandler"
    private val DATABASE_REF: DatabaseReference = FirebaseDatabase.getInstance().getReference("beacon_status")

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM message received: ${remoteMessage.data}")

        // Handle high-priority messages only
        if (remoteMessage.priority == RemoteMessage.Priority.HIGH) {
            handleHighPriorityMessage(remoteMessage.data)
        }
    }

    /**
     * Handle high-priority FCM message
     * These can trigger alarm even when app is in background
     */
    private fun handleHighPriorityMessage(data: Map<String, String>) {
        val action = data.action ?: ""

        when (action) {
            // Remote schedule control from partner's phone
            "update_schedule" -> handleScheduleUpdate(data)

            // Force 24/7 mode toggle
            "force_24_7" -> handleForce247Toggle(data)

            // Activate listening now (bypasses schedule)
            "activate_now" -> handleActivateNow(data)

            // General status update
            else -> Log.d(TAG, "Unknown FCM action: $action")
        }
    }

    /**
     * Handle schedule update from partner dashboard
     */
    private fun handleScheduleUpdate(data: Map<String, String>) {
        try {
            val startTime = data["start_time"] ?: return
            val endTime = data["end_time"] ?: return
            val mode = data["mode"] ?: "scheduled"

            // Update DataStore locally
            DataStoreManager.setScheduledWindow(this, startTime, endTime)
            DataStoreManager.setListeningMode(this, mode)

            // Update Firebase status
            DATABASE_REF.child("listening_mode").setValue(mode)
            DATABASE_REF.child("schedule_start").setValue(startTime)
            DATABASE_REF.child("schedule_end").setValue(endTime)

            Log.d(TAG, "Schedule updated: $startTime - $endTime, mode: $mode")

            // TODO: Show notification to user about schedule update
        } catch (e: Exception) {
            Log.e(TAG, "Error handling schedule update", e)
        }
    }

    /**
     * Handle force 24/7 mode toggle
     */
    private fun handleForce247Toggle(data: Map<String, String>) {
        try {
            val enable247 = data["enable"]?.let { it.toBoolean() } ?: true

            val mode = if (enable247) "24/7" else "scheduled"
            DataStoreManager.setListeningMode(this, mode)

            DATABASE_REF.child("listening_mode").setValue(mode)

            Log.d(TAG, "24/7 mode $mode - enabled: $enable247")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling 24/7 toggle", e)
        }
    }

    /**
     * Handle activate now (bypasses schedule)
     */
    private fun handleActivateNow(data: Map<String, String>) {
        try {
            // Immediately trigger the alarm service
            val alarmIntent = android.content.Intent(this, AlarmService::class.java)
            alarmIntent.setAction(AlarmService.ACTION_TRIGGER_IMMEDIATE)

            // Add data extras
            alarmIntent.putExtras(data)

            // Start foreground service
            ContextCompat.startForegroundService(this, alarmIntent)

            Log.d(TAG, "Activate now triggered - AlarmService started")
        } catch (e: Exception) {
            Log.e(TAG, "Error activating now", e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        // TODO: Update token in Firebase Realtime Database if needed
    }

    /**
     * Handle token refresh and subscribe to topic
     * Partners can publish to a topic to send commands to all beacons
     */
    override fun onTokenRefresh() {
        val token = firebaseAuth?.token ?: ""
        // Subscribe to partner command topic
        // FirebaseMessaging.getInstance().subscribeToTopic("beacon_commands")
        Log.d(TAG, "Token refreshed, subscribing to beacon commands topic")
    }
}

/**
 * Firebase Instance ID Receiver - Handles FCM token refresh events
 */
class FirebaseInstanceIdReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Trigger token refresh handling
        val service = com.monobeacon.receiver.FirebaseMessagingHandler::class.java
        // FirebaseInstanceId.getInstance().getToken()
        Log.d("FirebaseInstanceId", "Token refresh event received")
    }
}
