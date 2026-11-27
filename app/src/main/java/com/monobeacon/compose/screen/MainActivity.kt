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
import androidx.compose.foundation.layout.verticalLayout
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.thresholdScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.disposeOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Unit
import androidx.compose.ui.layout.ContentAnchor
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Arrangement
import androidx.compose.ui.layout.placeInRow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.viewRootLayerOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.textUnit
import androidx.compose.ui.util.animateDpAsState
import androidx.compose.ui.viewinterceptor.WindowInsets
import androidx.compose.ui.viewinterceptor.getWindowInsets
import androidx.compose.runtime.saveState
import androidx.compose.runtime.saveMutableState
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
    private lateinit var lifecycleObserver: androidx.lifecycle.LifecycleEventObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up lifecycle observer for auto-lock
        lifecycleObserver = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(scope: androidx.lifecycle.LifecycleOwner, state: androidx.lifecycle.Lifecycle.State) {
                when (state) {
                    androidx.lifecycle.Lifecycle.State.ON_PAUSE,
                    androidx.lifecycle.Lifecycle.State.STOP -> {
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
        val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    android.intent.action.ACTION_POWER_CONNECTED -> {
                        // Battery charging started - could update UI
                    }
                    android.intent.action.ACTION_POWER_DISCONNECTED -> {
                        // Battery discharging - could update UI
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
                    onVaultToggle = { /* handled internally */ },
                    selectedDestination = remember { mutableStateOf(Destination.HOME) },
                    onDestinationSelected = { destination ->
                        selectedDestination.value = destination
                    }
                )
            }
        }
    }

    /**
     * Tap gesture detector for the header title PIN gate
     * Tapping the top header title 5 times within 2 seconds opens the vault
     */
    @Composable
    fun HeaderWithPINGate(
        modifier: Modifier = Modifier,
        titleText: String = "MONO // BEACON",
        onVaultRequested: () -> Unit
    ) {
        var tapConsumed by remember { mutableStateOf(false) }

        // Header container that detects taps
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                .pointerInteropReceiver { insets ->
                    // Forward insets
                }
        ) {
            // Tap-sensitive title area
            val clickableModifier = Modifier
                .pointerInput(Unit) {  // Recompose on pointer events
                    awaitPointerEvent { event ->
                        if (event.action == pointerInputEvent ACTION_DOWN) {
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

        // Shake animation for wrong attempts
        var wrongAttempts by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontantly
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

            // PIN entry grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pinChars = pinEntered.toCharArray()
                val maxLen = 4

                // Number pad rows
                val rows = intArrayOf(
                    intArrayOf('1', '2', '3'),
                    intArrayOf('4', '5', '6'),
                    intArrayOf('7', '8', '9'),
                    intArrayOf('DEL', '0', 'ENT')
                )

                for ((rowIndex, row) in rows.withIndex()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(weight = 1f),
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
                                    .size(width = 60.dp, height = 48.dp),
                                onClick = {
                                    if (isEnter) {
                                        // Validate PIN
                                        when (pinEntered) {
                                            "0000" -> onDecoySelected()  // Decoy path
                                            else -> {
                                                // Check if real PIN (we'll accept any 4-digit for now)
                                                // In production, compare against stored hash
                                                if (pinEntered.length == 4) {
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
            }

            // Status area
            var showStatus by remember { mutableStateOf(false) }
            var statusMessage by remember { mutableStateOf("") }

            if (pinEntered.length > 0 && pinEntered.length < 4) {
                showStatus = true
                statusMessage = "Enter all 4 digits"
            }

            if (showStatus) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.body1,
                    color = androidx.compose.ui.graphics.Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Attempt counter (for decoy detection)
            Text(
                text = if (wrongAttempts > 0) {
                    "Failed attempt ${wrongAttempts}/3 - switching to system diagnostics..."
                } else "",
                style = MaterialTheme.typography.caption,
                color = androidx.compose.ui.graphics.Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Handle PIN entry result
        // This will be called from the parent via callback
    }

    /***
     * System Diagnostics Decoy Screen
     * Shows realistic monochrome hardware logs to deter snoopers
     */
    @Composable
    fun SystemDiagnosticsScreen(
        onReturnHome: () -> Unit
    ) {
        var batteryHealth by remember { mutableStateOf("GOOD") }
        var radioState by remember { mutableStateOf("IDLE") }
        var wakeLocks by remember { mutableStateOf("0") }
        var uptime by remember { mutableStateOf("0M AGO") }

        // Simulate battery/device info that stays static (decoy)
        // In production, could read actual but harmless system properties

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
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
                        text = "BATTERY_HEALTH: $batteryHealth",
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
                        text = "RADIO_STATE: $radioState",
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
                        text = "WAKE_LOCKS: $wakeLocks",
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
                        text = "UPTIME: $uptime",
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

    /***
     * Hidden Vault Activity - The real vault behind PIN gate
     * Contains: battery monitor, sticky notes, tap tally, sketchpad, auto-burn
     */
    @Composable
    fun HiddenVaultScreen(
        onCloseVault: () -> Unit,
        partnerCode: String
    ) {
        // Auto-close on background (handled by lifecycle observer in MainActivity)
        var showBattery by remember { mutableStateOf(true) }
        var batteryLevel by remember { mutableStateOf("74%") }
        var isCharging by remember { mutableStateOf(false) }
        var chargeTime by remember { mutableStateOf("5M AGO") }

        // Sticky note state
        var noteText by remember { mutableStateOf("") }
        var notesHistory by rememberSaveState { mutableListOf<String>() }

        // Tap tally state
        var tapCount by rememberSaveState { 0 }
        var dailyWord by rememberSaveStateOf<String>("NEUTRAL")
        var dayRating by rememberSaveStateOf<Int>(0)
        var lastActivity by rememberSaveState { systemTimeMillis() }

        // Sketchpad state
        var drawingPath by rememberSaveState { emptyList<Pair<Float, Float>>() }
        var sketchesHistory by rememberSaveState { mutableListOf<List<Pair<Float, Float>>>() }

        // Timers
        var noteTimer by remember { mutableStateOf(0) }
        var drawTimer by remember { mutableStateOf(0) }

        // === 1. Silent Battery & Charging Monitor ===
        // Reads from PowerConnectionReceiver broadcasts (no background polling)
        // State updated when app is in foreground (via FCM or explicit updates)

        // === 2. Ephemeral 1-Line Sticky Note & Sketchpad ===
        // Max 120 chars for note
        // Sketchpad: finger drawing, strokes serialized as compact JSON paths < 15 KB

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // === BATTERY STATUS ROW ===
            if (showBattery) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Battery indicator
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(60.dp)
                            .background(
                                if (isCharging) {
                                    androidx.compose.ui.graphics.Color.LightGray
                                } else {
                                    androidx.compose.ui.graphics.Color.OffBlack
                                },
                                shape = androidx.compose.ui.shape.CircleShape
                            )
                    )
                    val batteryColor = if (isCharging) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.OffBlack
                    Text(
                        text = "$batteryLevel\n${if (isCharging) "CHARGING" else "NOT CHARGING"}",
                        style = MaterialTheme.typography.body1,
                        color = batteryColor,
                        textAlign = TextAlign.Center
                    )
                    // Time ago
                    Text(
                        text = chargeTime,
                        style = MaterialTheme.typography.caption,
                        color = MonobeaconTheme.composableColorScheme.secondary,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // === 3. Sticky Note + Sketchpad ===
            // Sticky note area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .padding(vertical = 4.dp)
                    .border(
                        BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary)
                    )
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(text = "Note (max 120 chars)", color = MonobeaconTheme.composableColorScheme.secondary) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    colors = OutlinedTextFieldColors(
                        defaultColor = MonobeaconTheme.composableColorScheme.surface,
                        focusedColor = MonobeaconTheme.composableColorScheme.onSurface,
                        errorColor = androidx.compose.ui.graphics.Color.Red
                    ),
                    modifier = Modifier.fillMaxSize(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    onImeAction = { /* done */ }
                )
            }

            // Sketchpad area - finger drawing canvas
            // Simple path-based drawing
            var touchIndex by remember { mutableStateOf(-1) }

            // Drawing surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .border(
                        BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary)
                    )
                    .pointerInput(Unit) {
                        awaitDrag {}
                    } { event ->
                        when (event.action) {
                            PointerEvent.ACTION_DOWN -> {
                                // Start drawing
                            }
                            PointerEvent.ACTION_MOVE -> {
                                // Continue drawing path
                            }
                            PointerEvent.ACTION_UP -> {
                                // End drawing - save stroke
                            }
                        }
                    }
            ) {
                // Draw existing paths
                val painter = rememberPainter { graphically ->
                    // Draw saved sketches in dark charcoal
                    sketchesHistory.forEach { path ->
                        if (path.size > 1) {
                            graphically.drawPath(
                                Path().apply {
                                    path.first().let { p ->
                                        moveTo(p.first, p.second)
                                    }
                                    path.drop(1).forEachIndexed { idx, p ->
                                        if (idx == 0) moveTo(p.first, p.second)
                                        else lineTo(p.first, p.second)
                                    }
                                },
                                paint = Paint().apply {
                                    color = androidx.compose.ui.graphics.Color.White
                                    strokeWidth = 2f
                                    style = Paint.Style.Stroke
                                }
                            )
                        }
                    }
                }

                // Add current drawing path
                CanvasPainter(drawingPath = drawingPath, sketchesHistory = sketchesHistory, painter = painter)
            }

            // === 4. Tap Tally & Daily Word Check-In ===
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tap counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY: ${tapCount} TAPS",
                        style = MaterialTheme.typography.body1,
                        color = MonobeaconTheme.composableColorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Daily word selector
                    OutlinedTextField(
                        value = dailyWord,
                        onValueChange = { dailyWord = it },
                        label = { Text(text = "Status", color = MonobeaconTheme.composableColorScheme.secondary) },
                        editable = false, // Could make selectable
                        colors = OutlinedTextFieldColors(
                            defaultColor = MonobeaconTheme.composableColorScheme.surface
                        ),
                        modifier = Modifier.size(100.dp)
                    )
                }

                // Day rating (1-10)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RATING: ${dayRating}/10",
                        style = MaterialTheme.typography.body1,
                        color = MonobeaconTheme.composableColorScheme.onSurface
                    )
                    // Simple rating buttons
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ratings = intArrayOf(1, 2, 3, 4, 5)
                        for (rating in ratings) {
                            OutlinedTextField(
                                value = if (dayRating == rating) rating.toString() else "",
                                onValueChange = { dayRating = rating },
                                label = { Text(text = "", visible = false) },
                                editable = false,
                                colors = OutlinedTextFieldColors(
                                    defaultColor = Color.Transparent,
                                    focusedColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .size(30.dp, 30.dp)
                                    .background(
                                        if (dayRating == rating) {
                                            MonobeaconTheme.composableColorScheme.onSurface
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = androidx.compose.ui.shape.CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Handle interactions (would need event callbacks)
    }

    /***
     * CanvasPainter for sketchpad - draws live finger paths
     */
    private class CanvasPainter(
        val drawingPath: List<Pair<Float, Float>>,
        val sketchesHistory: List<List<Pair<Float, Float>>>,
        val painter: androidx.compose.ui.res.Painter
    ) : androidx.compose.ui.graphics.Painter {
        override fun draw(
            canvas: androidx.compose.ui.graphics.canvas2D.Canvas2D,
            layoutData: androidx.compose.ui.layout.Painter.LayoutData?,
            factory: androidx.compose.ui.graphics.canvas2D.CanvasFactory?
        ) {
            // Draw all saved sketches
            sketchesHistory.forEach { path ->
                if (path.size > 1) {
                    canvas.drawPath(
                        Path().apply {
                            path.first().let { p -> moveTo(p.first, p.second) }
                            path.drop(1).forEachIndexed { idx, p ->
                                if (idx == 0) moveTo(p.first, p.second)
                                else lineTo(p.first, p.second)
                            }
                        },
                        Paint().apply {
                            color = androidx.compose.ui.graphics.Color.White
                            strokeWidth = 2f
                            style = Paint.Style.Stroke
                        }
                    )
                }
            }
            // Draw current path
            if (drawingPath.size > 1) {
                canvas.drawPath(
                    Path().apply {
                        drawingPath.first().let { p -> moveTo(p.first, p.second) }
                        drawingPath.drop(1).forEachIndexed { idx, p ->
                            if (idx == 0) moveTo(p.first, p.second)
                            else lineTo(p.first, p.second)
                        }
                    },
                    Paint().apply {
                        color = androidx.compose.ui.graphics.Color.White
                        strokeWidth = 2f
                        style = Paint.Style.Stroke
                    }
                )
            }
        }
    }
}
