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

/**
 * SMS Trigger Receiver - Handles offline SMS commands
 * Works completely without internet/Wi-Fi/mobile data.
 * All SMS commands are case-insensitive.
 */
class SmsTriggerReceiver : BroadcastReceiver() {

    private val TAG = "SmsTriggerReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val messages = extractSmsMessages(intent, context)

        for (message in messages) {
            val sender = message.originatingAddress
            val text = message.messageBody!!

            Log.d(TAG, "SMS from: $sender, text: $text")

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

                else -> Log.w(TAG, "Unknown SMS command: $upperText")
            }
        }
    }

    /** Extract SMS messages from intent */
    private fun extractSmsMessages(intent: Intent, context: Context): Array<SmsMessage> {
        val extras = intent.extras
        if (extras == null) return emptyArray()

        if (extras.containsKey("pdus")) {
            val objects = extras.get("pdus") as Array<*>
            return objects.map { pdus ->
                SmsMessage.createFromPdu(pdus as ByteArray)
            } as Array<SmsMessage>
        }
        emptyArray()
    }

    /** Handle RINGNOW command */
    private fun handleRingNowCommand(sender: String, originalText: String, upperText: String) {
        Log.d(TAG, "Handling RINGNOW from: $sender")

        try {
            // Trigger alarm service immediately
            val alarmIntent = android.content.Intent(context, com.monobeacon.service.AlarmService::class.java)
            alarmIntent.action = com.monobeacon.service.AlarmService.ACTION_TRIGGER_IMMEDIATE
            alarmIntent.putExtra("sms_sender", sender)
            alarmIntent.putExtra("sms_original_text", originalText)
            alarmIntent.putExtra("command_type", "RINGNOW")
            alarmIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            context.startForegroundService(alarmIntent)

            // Update DataStore remote status (safe - no crash if Firebase unavailable)
            DataStoreManager.updateRemoteStatus(context, "listening_mode", "ring_now_triggered")

            // Send confirmation (optional)
            sendSmsConfirmation(context, sender, "Alarm triggered via SMS command")

            Log.d(TAG, "RINGNOW command processed")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering alarm from SMS", e)
        }
    }

    /** Handle BEACON ON command */
    private fun handleBeaconOnCommand(sender: String, upperText: String) {
        Log.d(TAG, "Handling BEACON ON from: $sender")
        DataStoreManager.setListeningMode(context, "24/7")
        DataStoreManager.updateRemoteStatus(context, "listening_mode", "24_7")
        sendSmsConfirmation(context, sender, "BEACON mode set to 24/7 Always Active")
    }

    /** Handle BEACON WINDOW command */
    private fun handleBeaconWindowCommand(sender: String, upperText: String) {
        Log.d(TAG, "Handling BEACON WINDOW from: $sender")
        val windowRegex = java.util.regex.Regex("BEACON WINDOW\\s+(\\d{2}:\\d{2})-(\\d{2}:\\d{2})")
        val match = windowRegex.matchEntire(upperText)
        if (match != null) {
            val startTime = match.groupValues[1]
            val endTime = match.groupValues[2]
            DataStoreManager.setScheduledWindow(context, startTime, endTime)
            DataStoreManager.setListeningMode(context, "scheduled")
            DataStoreManager.updateRemoteStatus(context, "listening_mode", "scheduled")
            val confirmation = "BEACON WINDOW updated: $startTime - $endTime"
            sendSmsConfirmation(context, sender, confirmation)
        } else {
            Log.w(TAG, "Invalid BEACON WINDOW format")
        }
    }

    /** Handle BEACON OFF command */
    private fun handleBeaconOffCommand(sender: String, upperText: String) {
        Log.d(TAG, "Handling BEACON OFF from: $sender")
        DataStoreManager.setListeningMode(context, "paused")
        DataStoreManager.updateRemoteStatus(context, "listening_mode", "paused")
        val confirmation = "BEACON OFF - Scheduled listening paused. Use RINGNOW to trigger alarm."
        sendSmsConfirmation(context, sender, confirmation)
    }

    /** Send silent SMS confirmation */
    private fun sendSmsConfirmation(context: Context, sender: String, message: String) {
        try {
            val confirmIntent = android.content.Intent(context, SmsConfirmationService::class.java)
            confirmIntent.putExtra("confirm_sender", sender)
            confirmIntent.putExtra("confirm_message", message)
            context.startService(confirmIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS confirmation", e)
        }
    }
}

/** SMS Confirmation Service */
class SmsConfirmationService : androidx.core.content.ContextCompat.startForegroundService