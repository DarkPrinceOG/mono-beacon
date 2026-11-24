package com.monobeacon.service

import android.content.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * AlarmService - Foreground service that triggers maximum-volume alarm,
 * flashlight strobe, and vibration pattern when phone is lost.
 * Runs as FOREGROUND_SERVICE_MEDIA_PLAYBACK to bypass Silent Mode and DND.
 */
class AlarmService : Service() {

    // Action constants
    companion object {
        const val ACTION_TRIGGER_IMMEDIATE = "com.monobeacon.action.TRIGGER_IMMEDIATE"
        const val ACTION_STOP = "com.monobeacon.action.STOP"
        const val ACTION_RESTORE_VOLUME = "com.monobeacon.action.RESTORE_VOLUME"

        // Extra keys
        const val EXTRA_SMS_SENDER = "extra_sms_sender"
        const val EXTRA_IS_REMOTE = "extra_is_remote"
        const val EXTRA_LISTENING_MODE = "extra_listening_mode"
    }

    // Alarm service state
    private var currentVolume: Int = 0
    private var currentRingtoneIndex: Int = 0
    private var vibrationTimer: android.os.Handler::? = null
    private var flashlightTimer: android.os.Handler::? = null
    private var isRunning = false

    // Volume restoration targets
    private var savedStreamType: Int = AudioManager.STREAM_ALARM
    private var savedVolume: Int = 0

    // Flashlight
    private var cameraManager: android.hardware.camera2CameraManager? = null
    private var isFlashlightOn = false

    // Vibration pattern
    private val VIBRATION_PATTERN_LONG = longArrayOf(0, 1000, 1000, 1000, 1000, 1000, 1000, 1000)
    private val VIBRATION_PATTERN_SHORT = longArrayOf(0, 200, 200, 200, 200, 200, 200, 200)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag("AlarmService").d("onStartCommand called, isRunning=$isRunning")

        if (intent != null) {
            val action = intent.action

            when (action) {
                ACTION_TRIGGER_IMMEDIATE -> {
                    Timber.tag("AlarmService").i("Trigger immediate alarm requested")
                    triggerAlarm()
                }
                ACTION_STOP -> {
                    Timber.tag("AlarmService").i("Stop alarm requested")
                    stopAlarm()
                }
                ACTION_RESTORE_VOLUME -> {
                    Timber.tag("AlarmService").i("Restore volume requested")
                    restorePreviousVolume()
                }
                else -> {
                    Timber.tag("AlarmService").w("Unknown action: $action")
                }
            }
        } else {
            // Default: trigger immediately if service is started without action
            Timber.tag("AlarmService").w("onStartCommand called without action, triggering default")
            triggerAlarm()
        }

        // Keep service running until explicitly stopped
        return START_STICKY
    }

    /**
     * Trigger the maximum-volume alarm with flashlight and vibration
     */
    private fun triggerAlarm() {
        if (isRunning) {
            Timber.tag("AlarmService").w("Alarm already running, skipping")
            return
        }

        isRunning = true
        Timber.tag("AlarmService").i("Starting alarm service - bypassing Silent/DND")

        try {
            // Save existing volume levels before overriding
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            savedStreamType = AudioManager.STREAM_ALARM
            savedVolume = audioManager.getStreamVolume(savedStreamType)

            // Force alarm stream to maximum volume
            val maxVolume = audioManager.getStreamMaxVolume(savedStreamType)
            audioManager.setStreamVolume(savedStreamType, maxVolume, 0)

            // Store original for later restoration
            currentVolume = savedVolume

            // Set alarm audio track that bypasses Silent Mode
            // Use RingtoneManager with TYPE_ALARM usage
            val ringtone = RingtoneManager.getRingtone(this, RingtoneManager.TYPE_ALARM)
            ringtone.play()

            // Start continuous alarm loop - loop the ringtone
            startAlarmLoop()

            // Start vibration
            startVibration(audioManager)

            // Start flashlight strobe
            startFlashlightStrobe()

            // Foreground service notification
            startForeground(
                ALARM_SERVICE_ID,
                createForegroundNotification()
            )

            Timber.tag("AlarmService").i("Alarm active - foreground service running")

        } catch (e: Exception) {
            Timber.tag("AlarmService").e("Error triggering alarm", e)
            stopSelf()
        }
    }

    /**
     * Start continuous alarm loop that bypasses Silent Mode
     */
    private fun startAlarmLoop() {
        // Use a handler to loop the ringtone
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postRepeated({
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                // Ensure volume stays at max
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

                // Re-trigger ringtone
                val ringtone = RingtoneManager.getRingtone(this, RingtoneManager.TYPE_ALARM)
                ringtone.play()
            } catch (e: Exception) {
                Timber.tag("AlarmService").e("Error in alarm loop", e)
            }
        }, 0, 1000) // Check every second
    }

    /**
     * Start heavy vibration pattern
     */
    private fun startVibration(audioManager: android.media.AudioManager) {
        val vibrator: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (vibrator.hasVibrator && vibrator.hasAmplitudeControl()) {
            // Use VibratorManager for API 26+
            val vibratorManager = getSystemService(VibratorManager::class.java)

            // Heavy vibration pattern: on/off cycles
            vibratorManager?.vibrate(
                VibrationEffect.createWaveform(
                    VIBRATION_PATTERN_LONG,    // pattern
                    0,                         // amplitude start
                    1.0f                       // amplitude end
                )
            )

            // Schedule next vibration cycle
            vibrationTimer = object : android.os.Handler(android.os.Looper.getMainLooper()) {
                handleMessage(null) {
                    startVibration(audioManager) // Recursive - continuous vibration
                }
            }
            vibrationTimer.postDelayed({
                sendMessage(null)
            }, 500) // 500ms cycle
        } else {
            // Fallback for older devices
            vibrator?.vibrate(VIBRATION_PATTERN_LONG, 0)
            vibrationTimer = object : android.os.Handler(android.os.Looper.getMainLooper()) {
                handleMessage(null) {
                    vibrator?.vibrate(VIBRATION_PATTERN_LONG, 0)
                    sendMessage(null)
                }
            }
            vibrationTimer.postDelayed({ sendMessage(null) }, 500)
        }
    }

    /**
     * Start LED/strobe flashlight every 400ms
     * Uses CameraManager.setTorchMode()
     */
    private fun startFlashlightStrobe() {
        cameraManager = getSystemService(android.hardware.camera2.CameraManager::class.java)

        try {
            // Find rear camera
            val cameraIds = cameraManager?.cameraIdList ?: return()
            val rearCameraId = cameraIds.find { it.contains("back") } ?: cameraIds.firstOrNull()

            if (rearCameraId != null) {
                // Start strobe: every 400ms
                flashlightTimer = object : android.os.Handler(android.os.Looper.getMainLooper()) {
                    var toggle = true

                    handleMessage(null) {
                        try {
                            camera_manager?.setTorchMode(
                                rearCameraId,
                                if (toggle) android.hardware.camera2.CameraManager.TORCH_MODE_ON
                                else android.hardware.camera2.CameraManager.TORCH_MODE_OFF
                            )
                            toggle = !toggle

                            // Schedule next 400ms toggle
                            sendMessage(null)
                        } catch (e: Exception) {
                            Timber.tag("AlarmService").e("Error toggling flashlight", e)
                        }
                    }

                    postDelayed({
                        sendMessage(null)
                    }, 400) // 400ms strobe cycle
                }
                flashlightTimer!!.postDelayed({ sendMessage(null) }, 400)
            }
        } catch (e: Exception) {
            Timber.tag("AlarmService").e("Error starting flashlight", e)
        }
    }

    /**
     * Stop the alarm and restore previous state
     */
    private fun stopAlarm() {
        Timber.tag("AlarmService").i("Stopping alarm service")

        isRunning = false

        // Stop alarm loop
        // Release ringtone
        try {
            val ringtone = RingtoneManager.getRingtone(this, RingtoneManager.TYPE_ALARM)
            ringtone.stop()
        } catch (e: Exception) {
            // Ignore
        }

        // Stop vibration
        try {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.cancel()
            // Also try old API
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator?.cancel()
        } catch (e: Exception) {
            // Ignore
        }

        // Stop flashlight
        try {
            camera_manager?.setTorchMode(
                cameraIds?.find { it.contains("back") } ?: "",
                android.hardware.camera2.CameraManager.TORCH_MODE_OFF
            )
        } catch (e: Exception) {
            // Ignore
        }

        // Restore previous volume levels
        restorePreviousVolume()

        // Stop foreground service
        stopForeground(true)

        // Stop self
        stopSelf()
    }

    /**
     * Restore previous volume levels before alarm override
     */
    private fun restorePreviousVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        try {
            // Restore saved volume
            audioManager.setStreamVolume(savedStreamType, savedVolume, 0)

            // Also restore ring stream if different
            val ringStream = AudioManager.STREAM_RING
            val savedRingVolume = audioManager.getStreamVolume(ringStream)
            audioManager.setStreamVolume(ringStream, savedRingVolume, 0)
        } catch (e: Exception) {
            Timber.tag("AlarmService").e("Error restoring volume", e)
        }
    }

    /**
     * Create foreground service notification
     */
    private fun createForegroundNotification(): android.app.Notification {
        val channelId = "monobeacon_alarm_channel"
        val channelName = "MONO // BEACON Alarm"

        // Create channel if on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = android.app.NotificationManager.IMPORTANCE_MAX
            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                importance
            ).apply {
                android.app.NotificationManager(
                    getSystemService(Context.NOTIFICATION_SERVICE)
                ).createNotificationChannel(channel)
            }
        }

        val builder = android.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_alarm_foreground)
            .setContentTitle("MONO // BEACON - Alarm Active")
            .setContentText("Phone is lost - Alarm ringing at maximum volume")
            .setColor(android.graphics.Color.WHITE)
            .setPriority(android.app.NotificationCompat.PRIORITY_MAX)
            .setShowWhen(false)
            .setOngoing(true)
            .setVisibility(android.app.NotificationCompat.VISIBILITY_PUBLIC)

        // Add action to stop alarm
        builder.addAction(
            android.app.NotificationCompat.Action(
                R.drawable.ic_stop_white,
                "STOP ALARM",
                android.content.Intent(this, AlarmService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        )

        return builder.build()
    }

    // Foreground service ID
    private val ALARM_SERVICE_ID = 1

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("AlarmService").i("AlarmService destroyed")

        // Clean up timers
        vibrationTimer?.removeCallbacksAndMessages(null)
        flashlightTimer?.removeCallbacksAndMessages(null)

        // Stop any ongoing vibration
        try {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.cancel()
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator?.cancel()
        } catch (e: Exception) {
            // Ignore
        }

        // Stop flashlight
        try {
            camera_manager?.setTorchMode(
                "",
                android.hardware.camera2.CameraManager.TORCH_MODE_OFF
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Required empty constructor for Android
    override fun onCreate() {
        super.onCreate()
    }
}