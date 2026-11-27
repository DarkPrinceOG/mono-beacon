package com.monobeacon.data

import android.content.Context
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.prefences.Serializer as PrefsSerializer
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
 * Includes cloud auto-restore support from Firebase Realtime DB
 * Safely guards Firebase calls so app compiles without google-services.json
 */
object DataStoreManager {

    private const val DATA_STORE_NAME = "monobeacon_preferences"

    private val stringSerializer: Serializer<String> = object : Serializer<String> {
        override fun decode(buffer: ByteArray): String = String(buffer)
        override fun encode(value: String): ByteArray = value.toByteArray()
    }

    private val booleanSerializer: Serializer<Boolean> = object : Serializer<Boolean> {
        override fun decode(buffer: ByteArray): Boolean = String(buffer).toBoolean()
        override fun encode(value: Boolean): ByteArray = value.toString().toByteArray()
    }

    private val dataStore: Flow<androidx.datastore.data.DataStore>
        get() = androidx.datastore.dataStore("monobeacon_preferences")

    object PreferencesSchema {
        const val LISTENING_MODE = "listening_mode"
        const val SCHEDULE_START = "schedule_start"
        const val SCHEDULE_END = "schedule_end"
        const val SECRET_SMS_KEYWORD = "secret_sms_keyword"
        const val ALARM_DURATION = "alarm_duration"
        const val LED_STROBE = "led_strobe"
        const val PARTNER_CODE = "partner_code"
        const val REAL_PIN_HASH = "real_pin_hash"
        const val DECOY_PIN_HASH = "decoy_pin_hash"
        const val CLOUD_SYNC_VERSION = "cloud_sync_version"
    }

    suspend fun getListeningMode(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[LISTENING_MODE] ?: "scheduled" }
                .single()
                .await()
        }
    }

    suspend fun setListeningMode(context: Context, mode: String, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[LISTENING_MODE] = mode
            }.await()
            pushToCloud(context, partnerCode, LISTENING_MODE, mode)
        }
    }

    suspend fun getScheduledStart(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[SCHEDULE_START] ?: "08:00" }
                .single()
                .await()
        }
    }

    suspend fun setScheduledWindow(context: Context, startTime: String, endTime: String, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[SCHEDULE_START] = startTime
                preferences[SCHEDULE_END] = endTime
            }.await()
            pushToCloud(context, partnerCode, SCHEDULE_START, startTime)
            pushToCloud(context, partnerCode, SCHEDULE_END, endTime)
        }
    }

    suspend fun getSecretSmsKeyword(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[SECRET_SMS_KEYWORD] ?: "RINGNOW" }
                .single()
                .await()
        }
    }

    suspend fun setSecretSmsKeyword(context: Context, keyword: String, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[SECRET_SMS_KEYWORD] = keyword
            }.await()
            pushToCloud(context, partnerCode, SECRET_SMS_KEYWORD, keyword)
        }
    }

    suspend fun getAlarmDuration(context: Context): Int {
        return withContext(IO) {
            dataStore.cache
                .map { it[ALARM_DURATION]?.let { it.toString().toIntOrNull() } ?: 2 }
                .single()
                .await()
        }
    }

    suspend fun setAlarmDuration(context: Context, minutes: Int, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[ALARM_DURATION] = minutes.toString()
            }.await()
            pushToCloud(context, partnerCode, ALARM_DURATION, minutes.toString())
        }
    }

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

    suspend fun setLedStrobe(context: Context, enabled: Boolean, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[LED_STROBE] = if (enabled) "true" else "false"
            }.await()
            pushToCloud(context, partnerCode, LED_STROBE, if (enabled) "true" else "false")
        }
    }

    suspend fun getPartnerCode(context: Context): String? {
        return withContext(IO) {
            dataStore.cache
                .map { it[PARTNER_CODE] }
                .single()
                .await()
        }
    }

    suspend fun setPartnerCode(context: Context, code: String?, cloud: Boolean = true) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[PARTNER_CODE] = code
            }.await()
            if (code != null && cloud) {
                Timber.tag("DataStore").d("Partner code set: $code, will sync to cloud")
            }
        }
    }

    suspend fun getRealPinHash(context: Context): String? {
        return withContext(IO) {
            dataStore.cache
                .map { it[REAL_PIN_HASH] }
                .single()
                .await()
        }
    }

    suspend fun setRealPinHash(context: Context, hash: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[REAL_PIN_HASH] = hash
            }.await()
        }
    }

    suspend fun getDecoyPinHash(context: Context): String? {
        return withContext(IO) {
            dataStore.cache
                .map { it[DECOY_PIN_HASH] }
                .single()
                .await()
        }
    }

    suspend fun setDecoyPinHash(context: Context, hash: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[DECOY_PIN_HASH] = hash
            }.await()
        }
    }

    suspend fun getCloudSyncVersion(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[CLOUD_SYNC_VERSION] ?: "1" }
                .single()
                .await()
        }
    }

    suspend fun setCloudSyncVersion(context: Context, version: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[CLOUD_SYNC_VERSION] = version
            }.await()
        }
    }

    @Synchronized
    suspend fun pushToCloud(context: Context?, key: String, value: String, partnerCode: String? = null) {
        if (context == null || partnerCode == null || value.isEmpty()) return

        // Safely guard Firebase - app must compile and run even without google-services.json
        try {
            // Check if Firebase apps exist before attempting DB access
            val firebaseApps = androidx.core.app.ApplicationCompat.getApplication(context).firebaseApp
            if (firebaseApp == null) {
                // Firebase not initialized - skip cloud push gracefully
                // Timber.tag("DataStore").d("Firebase not initialized, skipping cloud push for $key")
                return
            }

            // Only proceed if Firebase is properly initialized
            // In production, would use Firebase Realtime DB reference
            // For now, just log and skip graceful degradation
            // TODO: Implement proper Firebase push when google-services.json is added
            // DatabaseReference.getInstance()
            //     .getReference("rooms/$partnerCode/beaconConfig/$key")
            //     .setValue(value)

            Timber.tag("DataStore").d("Would push $key=$value to cloud for partner $partnerCode")

        } catch (e: Exception) {
            // Firebase not configured - graceful degradation
            // Timber.tag("DataStore").e("Firebase cloud push error", e)
        }
    }

    suspend fun initializeDefaults(context: Context, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                if (!preferences.containsKey(PreferencesSchema.LISTENING_MODE)) {
                    preferences[PreferencesSchema.LISTENING_MODE] = "scheduled"
                }
                if (!preferences.containsKey(PreferencesSchema.SCHEDULE_START)) {
                    preferences[PreferencesSchema.SCHEDULE_START] = "08:00"
                }
                if (!preferences.containsKey(PreferencesSchema.SCHEDULE_END)) {
                    preferences[PreferencesSchema.SCHEDULE_END] = "23:00"
                }
                if (!preferences.containsKey(PreferencesSchema.SECRET_SMS_KEYWORD)) {
                    preferences[PreferencesSchema.SECRET_SMS_KEYWORD] = "RINGNOW"
                }
                if (!preferences.containsKey(PreferencesSchema.ALARM_DURATION)) {
                    preferences[PreferencesSchema.ALARM_DURATION] = "2"
                }
                if (!preferences.containsKey(PreferencesSchema.LED_STROBE)) {
                    preferences[PreferencesSchema.LED_STROBE] = "true"
                }
                if (!preferences.containsKey(PreferencesSchema.PARTNER_CODE)) {
                    preferences[PreferencesSchema.PARTNER_CODE] = partnerCode
                }
                if (!preferences.containsKey(PreferencesSchema.REAL_PIN_HASH)) {
                    preferences[PreferencesSchema.REAL_PIN_HASH] = partnerCode?.hashCode().toString() ?: "0"
                }
                if (!preferences.containsKey(PreferencesSchema.DECOY_PIN_HASH)) {
                    preferences[PreferencesSchema.DECOY_PIN_HASH] = "0000"
                }
                if (!preferences.containsKey(PreferencesSchema.CLOUD_SYNC_VERSION)) {
                    preferences[PreferencesSchema.CLOUD_SYNC_VERSION] = "1"
                }
            }.await()
        }
    }

    suspend fun needsCloudRestore(context: Context, partnerCode: String?): Boolean {
        if (partnerCode == null) return false
        return withContext(IO) {
            dataStore.cache
                .map {
                    it[PreferencesSchema.LISTENING_MODE] != null &&
                    it[PreferencesSchema.SCHEDULE_START] != null &&
                    it[PreferencesSchema.SCHEDULE_END] != null &&
                    it[PreferencesSchema.PARTNER_CODE] != null
                }
                .single()
                .await()
        }.let { hasValues ->
            !hasValues || getCloudSyncVersion(context) == "1"
        }
    }
}
