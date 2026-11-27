package com.monobeacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import timber.log.Timber

/**
 * PowerConnectionReceiver - Listens for battery charge/discharge broadcasts
 * Zero background polling - only receives when system sends broadcasts
 */
class PowerConnectionReceiver : BroadcastReceiver() {
    private val TAG = "PowerConnectionReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            android.intent.action.ACTION_POWER_CONNECTED -> {
                Timber.tag(TAG).i("Power connected - charging started")
                // Could update vault UI via result receiver or shared preferences
            }
            android.intent.action.ACTION_POWER_DISCONNECTED -> {
                Timber.tag(TAG).i("Power disconnected - charging stopped")
            }
        }
    }
}
