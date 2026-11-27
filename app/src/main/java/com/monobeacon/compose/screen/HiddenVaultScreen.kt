package com.monobeacon.compose.screen

import androidx.compose.foundation.interaction.MemoryTaxonomy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.thresholdScroll
import androidx.compose.foundation.pointer.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime remember
import androidx.compose.savestate.saveable
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.path.PathKt
import androidx.compose.ui.draw.shape.CircleShape
import androidx.compose.ui.draw.shape.RoundedCornerShape
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.keyEvent
import androidx.compose.ui.input.pointer.*;
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.RowScope
import androidx.compose.ui.layout.Arrangement
import androidx.compose.ui.layout.ColumnScope
import androidx.compose.ui.layout.side
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.textUnit
import androidx.compose.ui.unit.ideogramDensity
import androidx.compose.ui.util.animateDpAsState
import androidx.compose.ui.util.insertPathNode
import androidx.compose.ui.util.applicator
import androidx.compose.ui.util.nestedScroll
import androidx.compose.ui.util.rememberDraggableState
import androidx.compose.ui.util.rememberSnapSize
import androidx.compose.ui.util.rememberScrollableState
import androidx.compose.ui.util.rememberScrollState
import androidx.compose.ui.util.rememberTextFieldState
import androidx.compose.ui.util.textSnapshot
import androidx.compose.ui.util.textThreshold
import androidx.compose.ui.viewinterceptor.WindowInsets
import androidx.compose.ui.viewinterceptor.getWindowInsets
import com.monobeacon.data.DataStoreManager
import com.monobeacon.data.PreferencesSchema
import com.monobeacon.theme.MonobeaconTheme
import com.monobeacon.R
import timber.log.Timber

/**
 * HiddenVaultScreen - The core vault composable behind PIN gate
 * Features:
 * 1. Silent battery & charging monitor (broadcast receiver, no polling)
 * 2. 1-line sticky note (max 120 chars)
 * 3. Finger-drawing sketchpad (< 15 KB serialized)
 * 4. Tap tally & daily 1-word check-in
 * 5. 7-day auto-burn cleanup
 */
@Composable
fun HiddenVaultScreen(
    onCloseVault: () -> Unit,
    partnerCode: String
) {
    // --- State Initialization ---

    // Battery state (updated via PowerConnectionReceiver broadcasts)
    var batteryLevel by rememberSaveStateOf("74%")
    var isCharging by rememberSaveStateOf(false)
    var chargeTime by rememberSaveStateOf("5M AGO")

    // Sticky note state
    var noteText by rememberSaveStateOf("")  // max 120 chars
    var notesHistory by rememberSaveState<MutableList<String>> { mutableListOf() }

    // Tap tally state
    var tapCount by rememberSaveStateOf(0)
    var dailyWord by rememberSaveStateOf<String>("NEUTRAL")
    var dayRating by rememberSaveStateOf<Int>(0)
    var lastActivityTimestamp by rememberSaveStateOf(System.currentTimeMillis())

    // Sketchpad state - finger drawing
    // Stores paths as compact lists of (x, y) pairs - typically < 15 KB
    var currentPath by rememberSaveStateOf<MutableList<Pair<Float, Float>>> { mutableListOf() }
    var sketchesHistory by rememberSaveState<MutableList<MutableList<Pair<Float, Float>>>> { mutableListOf() }

    // Auto-burn state
    var lastOpenTime by rememberSaveStateOf<Long>(0L)

    // --- 1. Silent Battery & Charging Monitor ---
    // Reads last known state from PowerConnectionReceiver
    // No background polling - state only updates when app is in foreground

    // In a full implementation, this would observe LiveData from a ViewModel
    // or receive updates via FCM. For now, we use saved state.

    // --- 2. Ephemeral 1-Line Sticky Note & Sketchpad ---

    // Sticky note area (max 120 chars)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
            .padding(vertical = 4.dp)
            .border(BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary))
    ) {
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it; if (noteText.length > 120) noteText = noteText.substring(0, 120) },
            label = { 
                Text(
                    text = "Note (max 120 chars)",
                    color = MonobeaconTheme.composableColorScheme.secondary
                ) 
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            editable = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences
            ),
            colors = OutlinedTextFieldColors(
                defaultColor = MonobeaconTheme.composableColorScheme.surface,
                focusedColor = MonobeaconTheme.composableColorScheme.onSurface
            ),
            modifier = Modifier.fillMaxSize(),
            onImeAction = { /* done action */ }
        )
    }

    // --- 3. Tap Tally & Daily 1-Word Check-In ---

    // Tap counter row
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "TODAY: ${tapCount} TAPS",
            style = MaterialTheme.typography.body1,
            color = MonobeaconTheme.composableColorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Daily word selector - compact
        OutlinedTextField(
            value = dailyWord,
            onValueChange = { dailyWord = it },
            label = { Text(text = "Status", color = MonobeaconTheme.composableColorScheme.secondary) },
            colors = OutlinedTextFieldColors(
                defaultColor = MonobeaconTheme.composableColorScheme.surface
            ),
            modifier = Modifier.size(80.dp)
        )
    }

    // Day rating (1-10 stars, shown as filled/empty circles)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RATING:",
            style = MaterialTheme.typography.body1,
            color = MonobeaconTheme.composableColorScheme.onSurface
        )

        val ratings = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .size(24.dp, 24.dp)
                        .background(
                            if (dayRating >= rating) {
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

    // --- 4. Sketchpad: Finger Drawing Canvas ---
    // Drawing area with finger input, strokes saved as compact path data

    // Drawing surface
    var touchStartBy remember { mutableStateOf(false) }
    var lastX by remember { mutableStateOf(0f) }
    var lastY by remember { mutableStateOf(0f) }

    // Capture drawing gestures
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
            .border(
                BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary)
            )
            .pointerInput(Unit) {
                awaitPointerInputCancellaton()
            } { event ->
                when (event.action) {
                    PointerInputEvent.ACTION_DOWN -> {
                        touchStartBy.value = true
                        lastX = event.relativeX
                        lastY = event.relativeY
                        currentPath.value = mutableListOf(Pair(event.relativeX, event.relativeY))
                    }
                    PointerInputEvent.ACTION_MOVE -> {
                        if (touchStartBy.value) {
                            val currentX = event.relativeX
                            val currentY = event.relativeY
                            currentPath.value?.add(Pair(currentX, currentY))
                            lastX = currentX
                            lastY = currentY
                        }
                    }
                    PointerInputEvent.ACTION_UP -> {
                        touchStartBy.value = false
                        // Save completed stroke to history (if meaningful length)
                        if (currentPath.value?.size >= 2) {
                            sketchesHistory.value = mutableListOf(currentPath.value) + sketchesHistory.value
                            // Trim history to prevent memory issues (keep last 10 sketches)
                            if (sketchesHistory.value.size > 10) {
                                sketchesHistory.value = sketchesHistory.value.take(10)
                            }
                            // Reset current path for next stroke
                            currentPath.value = mutableListOf()
                        }
                    }
                }
            }
    ) {
        // Draw all saved sketches (in dark charcoal/white)
        val painter = rememberPainter { graphically ->
            sketchesHistory.value.forEach { path ->
                if (path.size > 1) {
                    graphically.drawPath(
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
            // Draw current in-progress path
            if (currentPath.value?.size >= 2) {
                graphically.drawPath(
                    Path().apply {
                        currentPath.value.first().let { p -> moveTo(p.first, p.second) }
                        currentPath.value.drop(1).forEachIndexed { idx, p ->
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

    // --- 5. 7-Day Auto-Burn Cleanup ---
    // On vault open, check if last activity is older than 7 days
    // If so, clear stored data

    // Check auto-burn on screen enter
    DisposableEffect(Unit) {
        // Update last activity timestamp
        lastActivityTimestamp.value = System.currentTimeMillis()

        // Check if older than 7 days (604,800,000 ms)
        val SEVEN_DAYS_MS = 604800000L
        val timeSinceLastOpen = System.currentTimeMillis() - lastOpenTime.value

        if (lastOpenTime.value > 0L && timeSinceLastOpen > SEVEN_DAYS_MS) {
            // Auto-burn: clear all stored data
            notesHistory.value = mutableListOf()
            sketchesHistory.value = mutableListOf()
            noteText = ""
            tapCount = 0
            dailyWord = "NEUTRAL"
            dayRating = 0
            currentPath.value = mutableListOf()
            Timber.tag("Vault").i("7-day auto-burn: cleared stale data")

            // Show brief indication (could be a toast or log)
        }

        // Remember when vault was last opened
        lastOpenTime.value = System.currentTimeMillis()

        // This effect runs once when composable enters composition
        disposeOf {
            // Cleanup on composition disposal
        }
    }

    // --- UI Layout ---

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        // Header with close button
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HIDDEN VAULT",
                style = MaterialTheme.typography.h5,
                color = MonobeaconTheme.composableColorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onCloseVault() },
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MonobeaconTheme.composableColorScheme.onSurface
                )
            ) {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.body1,
                    color = MonobeaconTheme.composableColorScheme.onSurface
                )
            }
        }

        // Battery row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Battery indicator circle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .background(
                        if (isCharging) {
                            MonobeaconTheme.composableColorScheme.secondary
                        } else {
                            MonobeaconTheme.composableColorScheme.surface
                        },
                        shape = CircleShape
                    )
            )
            Text(
                text = "$batteryLevel\n${if (isCharging) "CHARGING" else "DISCHARGING"}",
                style = MaterialTheme.typography.body1,
                color = if (isCharging) MonobeaconTheme.composableColorScheme.secondary else MonobeaconTheme.composableColorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = chargeTime,
                style = MaterialTheme.typography.caption,
                color = MonobeaconTheme.composableColorScheme.secondary
            )
        }

        // Sticky note + sketchpad column
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Sticky note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .padding(vertical = 4.dp)
                    .border(BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary))
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { 
                        Text(
                            text = "Note (max 120 chars)",
                            color = MonobeaconTheme.composableColorScheme.secondary
                        ) 
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    editable = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldColors(
                        defaultColor = MonobeaconTheme.composableColorScheme.surface,
                        focusedColor = MonobeaconTheme.composableColorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Sketchpad
            Text(
                text = "SKETCHPAD - Draw with finger",
                style = MaterialTheme.typography.caption,
                color = MonobeaconTheme.composableColorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(backgroundColor = MonobeaconTheme.composableColorScheme.surface)
                    .border(BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary))
                    .pointerInput(Unit) {
                        awaitPointerInputCancellaton()
                    } { event ->
                        when (event.action) {
                            PointerInputEvent.ACTION_DOWN -> {
                                touchStartBy.value = true
                                lastX = event.relativeX
                                lastY = event.relativeY
                                currentPath.value = mutableListOf(Pair(event.relativeX, event.relativeY))
                            }
                            PointerInputEvent.ACTION_MOVE -> {
                                if (touchStartBy.value) {
                                    val currentX = event.relativeX
                                    val currentY = event.relativeY
                                    currentPath.value?.add(Pair(currentX, currentY))
                                    lastX = currentX
                                    lastY = currentY
                                }
                            }
                            PointerInputEvent.ACTION_UP -> {
                                touchStartBy.value = false
                                if (currentPath.value?.size >= 2) {
                                    sketchesHistory.value = mutableListOf(currentPath.value) + sketchesHistory.value
                                    if (sketchesHistory.value.size > 10) {
                                        sketchesHistory.value = sketchesHistory.value.take(10)
                                    }
                                    currentPath.value = mutableListOf()
                                }
                            }
                        }
                    }
                ) {
                    // Custom painter to draw sketches
                    val painter = rememberPainter { graphically ->
                        sketchesHistory.value.forEach { path ->
                            if (path.size > 1) {
                                graphically.drawPath(
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
                        if (currentPath.value?.size >= 2) {
                            graphically.drawPath(
                                Path().apply {
                                    currentPath.value.first().let { p -> moveTo(p.first, p.second) }
                                    currentPath.value.drop(1).forEachIndexed { idx, p ->
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
                    // Apply the painter
                    androidx.compose.ui.platform.LocalPainter.current = painter
                }
            }

            // Tap tally + word + rating (already drawn above in state section)
            // But let's re-display them at the bottom
        }

        // Bottom: summary info
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Tap counter
            Text(
                text = "TODAY: ${tapCount} TAPS",
                style = MaterialTheme.typography.body1,
                color = MonobeaconTheme.composableColorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Daily word
            OutlinedTextField(
                value = dailyWord,
                onValueChange = { dailyWord = it },
                label = { Text(text = "How are you?", color = MonobeaconTheme.composableColorScheme.secondary) },
                colors = OutlinedTextFieldColors(
                    defaultColor = MonobeaconTheme.composableColorScheme.surface
                ),
                modifier = Modifier.size(120.dp)
            )

            // Day rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "RATING:",
                    style = MaterialTheme.typography.body1,
                    color = MonobeaconTheme.composableColorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ratings = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                    .size(24.dp, 24.dp)
                                    .background(
                                        if (dayRating >= rating) {
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
    }
}
