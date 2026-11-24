package com.monobeacon.data

import android.content.Context
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.prefences.Serializer as PrefsSerializer
import com.monobeacon.data.PreferencesSchema.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.lang.Boolean.*
import java.lang.String.*
import java.util.concurrent.TimeUnit

/**
 * DataStore Manager - Handles persistent storage of app preferences
 * Uses Jetpack Preferences DataStore for light, reliable persistence
 */
object DataStoreManager {

    // DataStore file name
    private const val DATA_STORE_NAME = "monobeacon_preferences"

    // Serializers for DataStore types
    private val stringSerializer: Serializer<String> = PrimitiveSerializer(
        decoder = { it },
        encoder = { it.toByteArray() }
    )

    private val booleanSerializer: Serializer<Boolean> = PrimitiveSerializer(
        decoder = { text -> text == "true" },
        encoder = { it.toString().toByteArray() }
    )

    /**
     * Get DataStore flow
     */
    private val dataStore: Flow<androidx.datastore.data.DataStore>
        get() = androidx.datastore.dataStore(DATA_STORE_NAME)

    /**
     * Preferences schema - all persistent keys
     */
    object PreferencesSchema {
        const val LISTENING_MODE = "listening_mode"
        const val SCHEDULE_START = "schedule_start"
        const val SCHEDULE_END = "schedule_end"
        const val SECRET_SMS_KEYWORD = "secret_sms_keyword"
        const val ALARM_DURATION = "alarm_duration"
        const val LED_STROBE = "led_strobe"
        const val PARTNER_CODE = "partner_code"
    }

    /**
     * Get current listening mode from DataStore
     */
    suspend fun getListeningMode(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[LISTENING_MODE] ?: "scheduled" }
                .single()
                .await()
        }
    }

    /**
     * Set listening mode in DataStore
     */
    suspend fun setListeningMode(context: Context, mode: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[LISTENING_MODE] = mode
            }.await()
        }
    }

    /**
     * Get scheduled window start time
     */
    suspend fun getScheduledStart(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[SCHEDULE_START] ?: "08:00" }
                .single()
                .await()
        }
    }

    /**
     * Get scheduled window end time
     */
    suspend fun getScheduledEnd(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[SCHEDULE_END] ?: "23:00" }
                .single()
                .await()
        }
    }

    /**
     * Set scheduled window times
     */
    suspend fun setScheduledWindow(context: Context, startTime: String, endTime: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[SCHEDULE_START] = startTime
                preferences[SCHEDULE_END] = endTime
            }.await()
        }
    }

    /**
     * Get custom secret SMS keyword
     */
    suspend fun getSecretSmsKeyword(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[SECRET_SMS_KEYWORD] ?: "RINGNOW" }
                .single()
                .await()
        }
    }

    /**
     * Set custom secret SMS keyword
     */
    suspend fun setSecretSmsKeyword(context: Context, keyword: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[SECRET_SMS_KEYWORD] = keyword
            }.await()
        }
    }

    /**
     * Get alarm duration in minutes
     */
    suspend fun getAlarmDuration(context: Context): Int {
        return withContext(IO) {
            dataStore.cache
                .map { 
                    it[ALARM_DURATION]?.let { 
                        it.toString().toIntOrNull() ?: 2 
                    } ?: 2 
                }
                .single()
                .await()
        }
    }

    /**
     * Set alarm duration
     */
    suspend fun setAlarmDuration(context: Context, minutes: Int) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[ALARM_DURATION] = minutes.toString()
            }.await()
        }
    }

    /**
     * Get LED strobe preference
     */
    suspend fun isLedStrobeEnabled(context: Context): Boolean {
        return withContext(IO) {
            dataStore.cache
                .map { it[LED_STROBE] ?: "true" }
                .castToString()
                .map { it == "true" }
                .single()
                .await()
        }
    }

    /**
     * Set LED strobe preference
     */
    suspend fun setLedStrobe(context: Context, enabled: Boolean) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[LED_STROBE] = if (enabled) "true" else "false"
            }.await()
        }
    }

    /**
     * Get partner code (optional)
     */
    suspend fun getPartnerCode(context: Context): String? {
        return withContext(IO) {
            dataStore.cache
                .map { it[PARTNER_CODE] }
                .single()
                .await()
        }
    }

    /**
     * Set partner code
     */
    suspend fun setPartnerCode(context: Context, code: String?) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[PARTNER_CODE] = code
            }.await()
        }
    }

    /**
     * Update remote Firebase status when online
     */
    suspend fun updateRemoteStatus(context: Context, key: String, value: String) {
        // This can be called from SMS receiver or other components
        // Implementation would check connectivity and update Firebase
        // For now, we just log - actual FCM handling is in the service
        val tag = "DataStoreRemoteUpdate"
        android.util.Log.d(tag, "Remote update requested: $key = $value")
        // TODO: Add connectivity check and Firebase push logic
    }

    /**
     * Initialize DataStore with default values
     */
    suspend fun initializeDefaults(context: Context) {
        withContext(IO) {
            dataStore.update { preferences ->
                if (!preferences.containsKey(LISTENING_MODE)) {
                    preferences[LISTENING_MODE] = "scheduled"
                }
                if (!preferences.containsKey(SCHEDULE_START)) {
                    preferences[SCHEDULE_START] = "08:00"
                }
                if (!preferences.containsKey(SCHEDULE_END)) {
                    preferences[SCHEDULE_END] = "23:00"
                }
                if (!preferences.containsKey(SECRET_SMS_KEYWORD)) {
                    preferences[SECRET_SMS_KEYWORD] = "RINGNOW"
                }
                if (!preferences.containsKey(ALARM_DURATION)) {
                    preferences[ALARM_DURATION] = "2"
                }
                if (!preferences.containsKey(LED_STROBE)) {
                    preferences[LED_STROBE] = "true"
                }
            }.await()
        }
    }
}
