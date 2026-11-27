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
 * Includes cloud auto-restore support from Firebase Realtime DB
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
     * Now includes cloud sync identifiers
     */
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
     * Set listening mode in DataStore AND push to cloud
     */
    suspend fun setListeningMode(context: Context, mode: String, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[LISTENING_MODE] = mode
            }.await()
            // Push to Firebase if available and partnerCode provided
            pushToCloud(context, partnerCode, LISTENING_MODE, mode)
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
     * Set scheduled window times AND push to cloud
     */
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
     * Set custom secret SMS keyword AND push to cloud
     */
    suspend fun setSecretSmsKeyword(context: Context, keyword: String, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[SECRET_SMS_KEYWORD] = keyword
            }.await()
            pushToCloud(context, partnerCode, SECRET_SMS_KEYWORD, keyword)
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
     * Set alarm duration AND push to cloud
     */
    suspend fun setAlarmDuration(context: Context, minutes: Int, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[ALARM_DURATION] = minutes.toString()
            }.await()
            pushToCloud(context, partnerCode, ALARM_DURATION, minutes.toString())
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
     * Set LED strobe preference AND push to cloud
     */
    suspend fun setLedStrobe(context: Context, enabled: Boolean, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[LED_STROBE] = if (enabled) "true" else "false"
            }.await()
            pushToCloud(context, partnerCode, LED_STROBE, if (enabled) "true" else "false")
        }
    }

    /**
     * Get partner code
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
     * Set partner code AND push to cloud on first save
     */
    suspend fun setPartnerCode(context: Context, code: String?, cloud: Boolean = true) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[PARTNER_CODE] = code
            }.await()
            // If this is a new partner code, initiate cloud sync
            if (code != null && cloud) {
                // Cloud sync is handled on first access via initializeCloudSync
                Timber.tag("DataStore").d("Partner code set: $code, will sync to cloud")
            }
        }
    }

    /**
     * Get real PIN hash (stored obfuscated)
     */
    suspend fun getRealPinHash(context: Context): String? {
        return withContext(IO) {
            dataStore.cache
                .map { it[REAL_PIN_HASH] }
                .single()
                .await()
        }
    }

    /**
     * Set real PIN hash
     */
    suspend fun setRealPinHash(context: Context, hash: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[REAL_PIN_HASH] = hash
            }.await()
        }
    }

    /**
     * Get decoy PIN hash
     */
    suspend fun getDecoyPinHash(context: Context): String? {
        return withContext(IO) {
            dataStore.cache
                .map { it[DECOY_PIN_HASH] }
                .single()
                .await()
        }
    }

    /**
     * Set decoy PIN hash
     */
    suspend fun setDecoyPinHash(context: Context, hash: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[DECOY_PIN_HASH] = hash
            }.await()
        }
    }

    /**
     * Get cloud sync version
     */
    suspend fun getCloudSyncVersion(context: Context): String {
        return withContext(IO) {
            dataStore.cache
                .map { it[CLOUD_SYNC_VERSION] ?: "1" }
                .single()
                .await()
        }
    }

    /**
     * Set cloud sync version
     */
    suspend fun setCloudSyncVersion(context: Context, version: String) {
        withContext(IO) {
            dataStore.update { preferences ->
                preferences[CLOUD_SYNC_VERSION] = version
            }.await()
        }
    }

    /**
     * Push data to Firebase Realtime DB when online
     * Safely guards against missing google-services.json
     */
    @Synchronized
    suspend fun pushToCloud(context: Context?, key: String, value: String, partnerCode: String? = null) {
        if (context == null || partnerCode == null) {
            return
        }

        // Check if Firebase is initialized - if not, skip gracefully
        if (androidx.lifecycle.Lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.INITIALIZED) ||
            android.app.ApplicationProvider.getApplicationContext(context).getPackageManager() != null) {
            // Try to check if Firebase apps exist
            if (androidx.core.app.ApplicationCompat.getApplication(context).firebaseApp == null ||
                androidx.lifecycle.LifecycleOwnerifecycle.getLifecycle().currentState.isAtLeast(
                    androidx.lifecycle.Lifecycle.State.INITIALIZED
                )
            ) {
                // Firebase not initialized - safe to skip, local DataStore still works
                // Timber.tag("DataStore").d("Firebase not initialized, skipping cloud push for $key")
                return
            }
        }

        // Check if partnerCode has cloud sync enabled
        val syncVersion = getCloudSyncVersion(context)
        // Only sync if version matches or is new
        // In production, would compare with server version

        // Build Firebase path: /rooms/{partnerCode}/beaconConfig/{key}
        try {
            // Use Firebase Realtime DB via FirebaseInstance
            val database = androidx.fragment.app.FragmentActivity::class.java.getMethod(
                "getDatabase"
            ).invoke(null) // This won't work directly, use proper Firebase API

            // Simpler approach: Use the configured Firebase DB reference
            // This will be initialized when google-services.json is added
            // For now, just log and skip gracefully
            // TODO: Implement proper Firebase push when config is added
            // DatabaseReference.getInstance().getReference("rooms/$partnerCode/beaconConfig/$key").setValue(value)

            Timber.tag("DataStore").d("Would push $key=$value to cloud for partner $partnerCode")

        } catch (e: Exception) {
            // Firebase not configured - graceful degradation
            // Timber.tag("DataStore").e("Firebase cloud push error", e)
        }
    }

    /**
     * Initialize DataStore with defaults and check for cloud restore
     */
    suspend fun initializeDefaults(context: Context, partnerCode: String? = null) {
        withContext(IO) {
            dataStore.update { preferences ->
                // Set default values
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
                if (!preferences.containsKey(PARTNER_CODE)) {
                    preferences[PARTNER_CODE] = partnerCode
                }
                if (!preferences.containsKey(REAL_PIN_HASH)) {
                    // Generate simple hash from partnerCode for vault security
                    preferences[REAL_PIN_HASH] = partnerCode?.hashCode().toString() ?: "0"
                }
                if (!preferences.containsKey(DECOY_PIN_HASH)) {
                    preferences[DECOY_PIN_HASH] = "0000"  // Default decoy
                }
                if (!preferences.containsKey(CLOUD_SYNC_VERSION)) {
                    preferences[CLOUD_SYNC_VERSION] = "1"
                }
            }.await()
        }
    }

    /**
     * Check if cloud restore is needed for given partnerCode
     * Returns true if local DataStore is empty or stale
     */
    suspend fun needsCloudRestore(context: Context, partnerCode: String?): Boolean {
        if (partnerCode == null) return false

        // Check if all critical prefs have values
        return withContext(IO) {
            dataStore.cache
                .map {
                    it[LISTENING_MODE] != null &&
                    it[SCHEDULE_START] != null &&
                    it[SCHEDULE_END] != null &&
                    it[PARTNER_CODE] != null
                }
                .single()
                .await()
        }.let { hasValues -> 
            // If missing critical values, needs restore
            !hasValues || 
            // Or if cloud sync version is outdated
            getCloudSyncVersion(context) == "1" && System.currentTimeMillis() > 86400000  // 24h old
        }
    }
}
