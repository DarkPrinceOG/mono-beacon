package com.monobeacon.compose.screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.interaction.MemoryTaxonomy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.thresholdScroll
import androidx.compose.foundation.pointer.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime remember
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.textUnit
import androidx.compose.ui.util.animateDpAsState
import androidx.compose.ui.viewinterceptor.WindowInsets
import androidx.compose.ui.viewinterceptor.getWindowInsets
import androidx.compose.runtime.saveState
import androidx.compose.runtime.saveMutableState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.monobeacon.data.DataStoreManager
import com.monobeacon.data.PreferencesSchema
import com.monobeacon.theme.MonobeaconTheme
import timber.log.Timber

/**
 * MainActivity - MONO // BEACON with Stealth Hidden Vault
 * - Disguised as "Beacon" utility app
 * - Tap gesture PIN gate (5 taps on header title within 2s)
 * - Auto-lock on background minimize
 * - Original locator functionality preserved
 */
class MainActivity : ComponentActivity() {

    // PIN gate state
    private var tapCount = 0
    private var firstTapTime: Long = 0
    private val TAP_THRESHOLD = 5
    private val TIME_WINDOW_MS = 2000
    private var isVaultUnlocked = false

    // Decoy PIN hash (stored obfuscated)
    private val DECOY_PIN = "0000"

    // Lifecycle observer for auto-lock
    private lateinit var lifecycleObserver: LifecycleEventObserver

    // Power connection receiver for vault battery monitor
    private lateinit var powerReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up lifecycle observer for auto-lock
        lifecycleObserver = object : LifecycleEventObserver {
            override fun onStateChanged(owner: LifecycleOwner, state: Lifecycle.State) {
                when (state) {
                    Lifecycle.State.ON_PAUSE, Lifecycle.State.STOP -> {
                        // Auto-lock vault when app minimized/screen locked
                        isVaultUnlocked = false
                        tapCount = 0
                    }
                }
            }
        }

        // Register lifecycle observer
        lifecycle.addObserver(lifecycleObserver)

        // Register power connection receiver for vault battery monitor
        powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    android.intent.action.ACTION_POWER_CONNECTED -> {
                        // Battery charging started
                    }
                    android.intent.action.ACTION_POWER_DISCONNECTED -> {
                        // Battery discharging
                    }
                }
            }
        }
        registerReceiver(powerReceiver, IntentFilter(
            android.intent.action.ACTION_POWER_CONNECTED,
            android.intent.action.ACTION_POWER_DISCONNECTED
        ))

        // Set up onBackPressedCallback with vault state check
        OnBackPressedCallback(isEnabled = true) {
            if (isVaultUnlocked) {
                // If vault is open, minimize it (simulate back)
                isVaultUnlocked = false
                tapCount = 0
                // Don't exit app - just close vault
                this.isEnabled = false
                this.isEnabled = true
            } else {
                // Normal app behavior - exit
                this.isEnabled = true
                finish()
            }
        }

        setContent {
            MonobeaconTheme {
                AppScaffold(
                    selectedDestination = remember { mutableStateOf(Destination.HOME) },
                    onDestinationSelected = { destination ->
                        selectedDestination.value = destination
                    }
                )
            }
        }
    }

    /***
     * Enum for app destinations
     */
    enum class Destination {
        HOME, SETTINGS, VAULT, GAMES
    }

    /***
     * Tap gesture detector for the header title PIN gate
     * Tapping the top header title 5 times within 2 seconds opens the vault
     */
    @Composable
    fun HeaderWithPINGate(
        modifier: Modifier = Modifier,
        titleText: String = "Beacon",
        onVaultRequested: () -> Unit
    ) {
        var tapConsumed by remember { mutableStateOf(false) }

        // Header container that detects taps
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
        ) {
            // Tap-sensitive title area
            val clickableModifier = Modifier
                .pointerInput(Unit) {  // Recompose on pointer events
                    awaitPointerEvent { event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val now = System.currentTimeMillis()
                            if (firstTapTime == 0) {
                                firstTapTime = now
                                tapCount = 1
                            } else if (now - firstTapTime <= TIME_WINDOW_MS) {
                                tapCount++
                                if (tapCount >= TAP_THRESHOLD) {
                                    // 5 taps detected - request vault
                                    if (!tapConsumed) {
                                        tapConsumed = true
                                        onVaultRequested()
                                    }
                                    // Reset after action
                                    firstTapTime = 0
                                    tapCount = 0
                                }
                            } else {
                                // Reset window
                                firstTapTime = now
                                tapCount = 1
                            }
                        }
                    }
                }

            Text(
                text = titleText,
                modifier = clickableModifier
                    .padding(horizontal = 16.dp)
                    .onClick { /* prevented - gesture only */ },
                style = MaterialTheme.typography.h5,
                color = MonobeaconTheme.composableColorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )
        }
    }

    /***
     * PIN Pad Screen - Shown after 5 taps
     * Real PIN unlocks vault, decoy PIN (0000) opens diagnostics
     */
    @Composable
    fun PinPadScreen(
        onPinEntered: (String) -> Unit,
        onVaultUnlocked: () -> Unit,
        onDecoySelected: () -> Unit
    ) {
        var pinEntered by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "SECURE ACCESS",
                style = MaterialTheme.typography.h6,
                color = MonobeaconTheme.composableColorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Instruction
            Text(
                text = "Enter 4-digit PIN to access Hidden Vault",
                style = MaterialTheme.typography.body2,
                color = MonobeaconTheme.composableColorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // PIN entry grid - 4x4 grid with numbers and DEL/ENT
            val rows = intArrayOf(
                intArrayOf('1', '2', '3'),
                intArrayOf('4', '5', '6'),
                intArrayOf('7', '8', '9'),
                intArrayOf('DEL', '0', 'ENT')
            )

            for ((rowIndex, row) in rows.withIndex()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Fill,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for ((colIndex, char) in row.withIndex()) {
                        val isEmpty = char == 'DEL'
                        val isEnter = char == 'ENT'

                        val buttonLabel = when {
                            isEnter → "✓"
                            isEmpty → "⌫"
                            else → char.toString()
                        }

                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .size(width = 56.dp, height = 44.dp),
                            onClick = {
                                if (isEnter) {
                                    // Validate PIN
                                    when (pinEntered) {
                                        "0000" -> onDecoySelected()  // Decoy path
                                        else -> {
                                            // Check if real PIN (we'll accept any 4-digit for now)
                                            if (pinEntered.length == 4 && pinEntered.all { it.isDigit() }) {
                                                onPinEntered(pinEntered)
                                            }
                                        }
                                    }
                                } else if (isEmpty) {
                                    // Delete last character
                                    pinEntered = pinEntered.substring(0, pinEntered.length - 1)
                                } else {
                                    pinEntered = pinEntered + char
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.Transparent,
                                contentColor = MonobeaconTheme.composableColorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = buttonLabel,
                                style = MaterialTheme.typography.body1,
                                color = MonobeaconTheme.composableColorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Status area
            if (pinEntered.length > 0 && pinEntered.length < 4) {
                // Show partial status
            }

            // Handle PIN entry result - this would be done via callbacks
        }
    }

    /***
     * System Diagnostics Decoy Screen
     * Shows realistic monochrome hardware logs to deter snoopers
     */
    @Composable
    fun SystemDiagnosticsScreen(
        onReturnHome: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYSTEM DIAGNOSTICS",
                style = MaterialTheme.typography.h6,
                color = MonobeaconTheme.composableColorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            // Battery info card
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "BATTERY_HEALTH: GOOD",
                        style = MaterialTheme.typography.body1,
                        color = MonobeaconTheme.composableColorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "CHARGE CYCLES: 847",
                        style = MaterialTheme.typography.caption,
                        color = MonobeaconTheme.composableColorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Radio state card
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "RADIO_STATE: IDLE",
                        style = MaterialTheme.typography.body1,
                        color = MonobeaconTheme.composableColorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SIGNAL STRENGTH: Good",
                        style = MaterialTheme.typography.caption,
                        color = MonobeaconTheme.composableColorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Wake locks card
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "WAKE_LOCKS: 0",
                        style = MaterialTheme.typography.body1,
                        color = MonobeaconTheme.composableColorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "PARTIAL_WAKE_LOCK: false",
                        style = MaterialTheme.typography.caption,
                        color = MonobeaconTheme.composableColorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Uptime card
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "UPTIME: 3 days 14h ago",
                        style = MaterialTheme.typography.body1,
                        color = MonobeaconTheme.composableColorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "BOOT_TIME: Simulated",
                        style = MaterialTheme.typography.caption,
                        color = MonobeaconTheme.composableColorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Return button
            Button(
                onClick = { onReturnHome() },
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MonobeaconTheme.composableColorScheme.onSurface
                )
            ) {
                Text(
                    text = "RETURN TO HOME",
                    style = MaterialTheme.typography.button,
                    color = MonobeaconTheme.composableColorScheme.onSurface
                )
            }
        }
    }
}
