package com.monobeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.content.ContextCompat
import com.monobeacon.data.DataStoreManager
import com.monobeacon.data.PreferencesSchema
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

/**
 * SMS Trigger Receiver - Handles offline SMS commands
 * Works completely without internet/Wi-Fi/mobile data.
 * Supports case-insensitive SMS commands for remote control.
 */
class SmsTriggerReceiver : BroadcastReceiver() {

    private val TAG = "SmsTriggerReceiver"
    private val DATABASE_REF: DatabaseReference = FirebaseDatabase.getInstance().getReference("beacon_status")

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS trigger receiver received")

        try {
            val messages: Array<SmsMessage> = extractSmsMessages(intent, context)

            for (message in messages) {
                val sender = message.getOriginatingAddress()
                val text = message.messageBody!!

                Log.d(TAG, "SMS from: $sender, text: $text")

                // Parse and handle commands (case-insensitive)
                val upperText = text.trim().toUpperCase()

                when {
                    // Command: RINGNOW (or custom keyword) - Triggers alarm immediately
                    upperText.startsWith("RINGNOW") -> handleRingNowCommand(sender, text, upperText)

                    // Command: BEACON ON / BEACON 24/7 - Sets Always Active mode
                    upperText.startsWith("BEACON ON") || upperText.startsWith("BEACON 24/7") ->
                        handleBeaconOnCommand(sender, upperText)

                    // Command: BEACON WINDOW HH:MM-HH:MM - Updates scheduled times
                    upperText.startsWith("BEACON WINDOW") -> handleBeaconWindowCommand(sender, upperText)

                    // Command: BEACON OFF - Pauses scheduled listening
                    upperText.startsWith("BEACON OFF") -> handleBeaconOffCommand(sender, upperText)

                    else -> {
                        // Unknown command - could be custom keyword handling
                        Log.w(TAG, "Unknown SMS command: $upperText")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }

    /**
     * Extract SMS messages from the intent
     */
    private fun extractSmsMessages(intent: Intent, context: Context): Array<SmsMessage> {
        val extras = intent.extras

        if (extras == null) {
            return emptyArray()
        }

        // Check for both new and legacy SMS formats
        if (extras.containsKey("pdus")) {
            val objects = extras.get("pdus") as Array<*>

            return objects.map { pdus ->
                SmsMessage.createFromPdu(pdus as ByteArray)
            } as Array<SmsMessage>
        } else if (extras.containsKey("format")) {
            // Legacy format
            val format = extras.getString("format")!!
            val messageBody = extras.getString("sms_body")!!
            arrayOf(SmsMessage.createFromPdu(
                /* pdus */ null,
                /* format */ null,
                /* null */ null,
                messageBody.toByteArray()
            ))
        } else {
            return emptyArray()
        }
    }

    /**
     * Handle RINGNOW command - Triggers alarm immediately, bypassing schedule
     */
    private fun handleRingNowCommand(sender: String, originalText: String, upperText: String) {
        Log.d(TAG, "Handling RINGNOW command from: $sender")

        try {
            // Trigger the AlarmService immediately via explicit intent
            val alarmIntent = android.content.Intent(context, AlarmService::class.java)
            alarmIntent.setAction(AlarmService.ACTION_TRIGGER_IMMEDIATE)

            // Add sender info as extra for logging/confirmation
            alarmIntent.putExtra("sms_sender", sender)
            alarmIntent.putExtra("sms_original_text", originalText)
            alarmIntent.putExtra("command_type", "RINGNOW")

            // Set flags for proper service startup
            alarmIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            // Start the foreground service
            context.startForegroundService(alarmIntent)

            // Update Firebase status if online
            DataStoreManager.updateRemoteStatus(context, "listening_mode", "ring_now_triggered")

            // Send silent confirmation (optional)
            sendSmsConfirmation(context, sender, "Alarm triggered via SMS command")

            Log.d(TAG, "RINGNOW command processed - AlarmService started")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering alarm from SMS", e)
        }
    }

    /**
     * Handle BEACON ON / BEACON 24/7 command - Sets Always Active mode
     */
    private fun handleBeaconOnCommand(sender: String, upperText: String) {
        Log.d(TAG, "Handling BEACON ON command from: $sender")

        try {
            // Update local DataStore
            DataStoreManager.setListeningMode(context, "24/7")

            // Push remote update to Firebase if online
            DATABASE_REF.child("listening_mode").setValue("24_7")

            // Send confirmation SMS
            sendSmsConfirmation(context, sender, "BEACON mode set to 24/7 Always Active")

            Log.d(TAG, "BEACON ON command processed - mode set to 24/7")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling BEACON ON command", e)
        }
    }

    /**
     * Handle BEACON WINDOW HH:MM-HH:MM command - Updates scheduled listening times
     */
    private fun handleBeaconWindowCommand(sender: String, upperText: String) {
        Log.d(TAG, "Handling BEACON WINDOW command from: $sender")

        try {
            // Extract window times from command: "BEACON WINDOW 09:00-22:00"
            val windowRegex = java.util.regex.Regex("BEACON WINDOW\\s+(\\d{2}:\\d{2})-(\\d{2}:\\d{2})")
            val match = windowRegex.matchEntire(upperText)

            if (match != null) {
                val startTime = match.groupValues[1]  // e.g., "09:00"
                val endTime = match.groupValues[2]    // e.g., "22:00"

                // Update DataStore with new schedule
                DataStoreManager.setListeningMode(context, "scheduled")
                DataStoreManager.setScheduledWindow(
                    context,
                    startTime,
                    endTime
                )

                // Push to Firebase
                DATABASE_REF.child("listening_mode").setValue("scheduled")
                DATABASE_REF.child("schedule_start").setValue(startTime)
                DATABASE_REF.child("schedule_end").setValue(endTime)

                // Send confirmation with window times
                val confirmation = "BEACON WINDOW updated: $startTime - $endTime"
                sendSmsConfirmation(context, sender, confirmation)

                Log.d(TAG, "BEACON WINDOW command processed - $startTime - $endTime")
            } else {
                Log.w(TAG, "Invalid BEACON WINDOW format in SMS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling BEACON WINDOW command", e)
        }
    }

    /**
     * Handle BEACON OFF command - Pauses scheduled listening
     */
    private fun handleBeaconOffCommand(sender: String, upperText: String) {
        Log.d(TAG, "Handling BEACON OFF command from: $sender")

        try {
            // Pause scheduled listening in DataStore
            DataStoreManager.setListeningMode(context, "paused")

            // Update Firebase
            DATABASE_REF.child("listening_mode").setValue("paused")

            // Send confirmation - still allows explicit RINGNOW override
            val confirmation = "BEACON OFF - Scheduled listening paused. Use RINGNOW to trigger alarm."
            sendSmsConfirmation(context, sender, confirmation)

            Log.d(TAG, "BEACON OFF command processed - schedule paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling BEACON OFF command", e)
        }
    }

    /**
     * Send silent SMS confirmation to the sender
     * This is optional and only works if we have permissions
     */
    private fun sendSmsConfirmation(context: Context, sender: String, message: String) {
        try {
            // Send a silent confirmation back to the sender
            val confirmIntent = android.content.Intent(context, SmsConfirmationService::class.java)
            confirmIntent.putExtra("confirm_sender", sender)
            confirmIntent.putExtra("confirm_message", message)
            context.startService(confirmIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS confirmation", e)
        }
    }
}

/**
 * SMS Confirmation Service - Sends confirmation response via SMS
 * Runs as a separate service to avoid blocking the receiver
 */
class SmsConfirmationService : androidx.core.content.ContextCompat.startForegroundService