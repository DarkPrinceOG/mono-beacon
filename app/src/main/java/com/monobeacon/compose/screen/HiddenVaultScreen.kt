package com.monobeacon.compose.screen

import androidx.compose.foundation.interaction.MemoryTaxonomy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pointer.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveState.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.path.PathKt
import androidx.compose.ui.draw.shape.CircleShape
import androidx.compose.ui.draw.shape.RoundedCornerShape
import androidx.compose.ui.input.key.keyEvent
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.textUnit
import androidx.compose.runtime.saveState.saveable
import com.monobeacon.data.DataStoreManager
import com.monobeacon.data.PreferencesSchema
import com.monobeacon.theme.MonobeaconTheme
import timber.log.Timber

/**
 * HiddenVaultScreen - Core vault behind PIN gate
 * Features: battery monitor, sticky note, sketchpad, tap tally, daily word, auto-burn
 */
@Composable
fun HiddenVaultScreen(
    onCloseVault: () -> Unit,
    partnerCode: String
) {
    // --- State Initialization ---

    // Battery state (updated via PowerConnectionReceiver broadcasts, no polling)
    var batteryLevel by rememberSaveStateOf("74%")
    var isCharging by rememberSaveStateOf(false)
    var chargeTime by rememberSaveStateOf("5M AGO")

    // Sticky note state (max 120 chars)
    var noteText by rememberSaveStateOf("")
    var notesHistory by rememberSaveState<MutableList<String>> { mutableListOf() }

    // Tap tally state
    var tapCount by rememberSaveStateOf(0)
    var dailyWord by rememberSaveStateOf<String>("NEUTRAL")
    var dayRating by rememberSaveStateOf<Int>(0)
    var lastActivityTimestamp by rememberSaveStateOf<Long>(0L)

    // Sketchpad state - finger drawing, paths saved as compact JSON < 15 KB
    var currentPath by rememberSaveStateOf<MutableList<Pair<Float, Float>>> { mutableListOf() }
    var sketchesHistory by rememberSaveState<MutableList<MutableList<Pair<Float, Float>>>> { mutableListOf() }

    // Auto-burn state
    var lastOpenTime by rememberSaveStateOf<Long>(0L)

    // --- 1. Silent Battery & Charging Monitor ---
    // Reads last known state from PowerConnectionReceiver broadcasts
    // No background polling - state only updates when app is in foreground

    // --- 2. 1-Line Sticky Note & Sketchpad ---

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
            label = { Text(text = "Note (max 120 chars)", color = MonobeaconTheme.composableColorScheme.secondary) },
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

    // --- 3. Tap Tally & Daily Word Check-In ---

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
        // Daily word selector
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

    // Day rating (1-10 as circles)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "RATING:", style = MaterialTheme.typography.body1,
            color = MonobeaconTheme.composableColorScheme.onSurface)
        val ratings = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.size(24.dp, 24.dp).background(
                        if (dayRating >= rating) MonobeaconTheme.composableColorScheme.onSurface else Color.Transparent,
                        shape = androidx.compose.ui.shape.CircleShape
                    )
                )
            }
        }
    }

    // --- 4. Sketchpad: Finger Drawing Canvas ---

    var touchStartBy remember mutableStateOf(false)
    var lastX remember 0f
    var lastY remember 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
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
        // Draw all saved sketches
        val painter = rememberPainter { graphically ->
            sketchesHistory.value.forEach { path ->
                if (path.size > 1) {
                    graphically.drawPath(
                        Path().apply {
                            path.first().let { p -> moveTo(p.first, p.second) }
                            path.drop(1).forEachIndexed { idx, p ->
                                if (idx == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second)
                            }
                        },
                        Paint().apply {
                            color = Color.White
                            strokeWidth = 2f
                            style = Paint.Style.Stroke
                        }
                    )
                }
            }
            if (currentPath.value?.size >= 2) {
                graphically.drawPath(
                    Path().apply {
                        currentPath.value.first().let { p -> moveTo(p.first, p.second) }
                        currentPath.value.drop(1).forEachIndexed { idx, p ->
                            if (idx == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second)
                        }
                    },
                    Paint().apply {
                        color = Color.White
                        strokeWidth = 2f
                        style = Paint.Style.Stroke
                    }
                )
            }
        }
    }

    // --- 5. 7-Day Auto-Burn Cleanup ---

    DisposableEffect(Unit) {
        lastActivityTimestamp.value = System.currentTimeMillis()
        val SEVEN_DAYS_MS = 604800000L
        val timeSinceLastOpen = System.currentTimeMillis() - lastOpenTime.value

        if (lastOpenTime.value > 0L && timeSinceLastOpen > SEVEN_DAYS_MS) {
            notesHistory.value = mutableListOf()
            sketchesHistory.value = mutableListOf()
            noteText = ""
            tapCount = 0
            dailyWord = "NEUTRAL"
            dayRating = 0
            currentPath.value = mutableListOf()
            Timber.tag("Vault").i("7-day auto-burn: cleared stale data")
        }

        lastOpenTime.value = System.currentTimeMillis()
        disposeOf {}
    }

    // --- UI Layout ---

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        // Header with close button
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "HIDDEN VAULT", style = MaterialTheme.typography.h5,
                color = MonobeaconTheme.composableColorScheme.onSurface, modifier = Modifier.weight(1f))
            Button(onClick = { onCloseVault() }, colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = MonobeaconTheme.composableColorScheme.onSurface
            )) {
                Text(text = "×", style = MaterialTheme.typography.body1, color = MonobeaconTheme.composableColorScheme.onSurface)
            }
        }

        // Battery row
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(width = 40.dp, height = 40.dp,
                background = if (isCharging) MonobeaconTheme.composableColorScheme.secondary else MonobeaconTheme.composableColorScheme.surface,
                shape = CircleShape
            )
            Text(text = "$batteryLevel\n${if (isCharging) "CHARGING" else "DISCHARGING"}",
                style = MaterialTheme.typography.body1,
                color = if (isCharging) MonobeaconTheme.composableColorScheme.secondary else MonobeaconTheme.composableColorScheme.onSurface,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(text = chargeTime, style = MaterialTheme.typography.caption,
                color = MonobeaconTheme.composableColorScheme.secondary)
        }

        // Sticky note + sketchpad column
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            // Sticky note
            Box(modifier = Modifier.fillMaxWidth().height(50.dp), background = MonobeaconTheme.composableColorScheme.surface,
                padding = 4.dp, border = BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary)) {
                OutlinedTextField(value = noteText, onValueChange = { noteText = it },
                    label = { Text(text = "Note (max 120 chars)", color = MonobeaconTheme.composableColorScheme.secondary) },
                    maxLines = 1, overflow = TextOverflow.Ellipsis, editable = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldColors(defaultColor = MonobeaconTheme.composableColorScheme.surface,
                        focusedColor = MonobeaconTheme.composableColorScheme.onSurface), modifier = Modifier.fillMaxSize())
            }

            // Sketchpad
            Text(text = "SKETCHPAD - Draw with finger", style = MaterialTheme.typography.caption,
                color = MonobeaconTheme.composableColorScheme.secondary, padding = 4.dp)

            Box(modifier = Modifier.fillMaxWidth().height(220.dp), background = MonobeaconTheme.composableColorScheme.surface,
                border = BorderStroke(1.dp, MonobeaconTheme.composableColorScheme.secondary))
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
                                if (sketchesHistory.value.size > 10) sketchesHistory.value = sketchesHistory.value.take(10)
                                currentPath.value = mutableListOf()
                            }
                        }
                    }
                ) {
                    // Custom painter to draw sketches
                    val painter = rememberPainter { graphically ->
                        sketchesHistory.value.forEach { p ->
                            if (p.size > 1) {
                                graphically.drawPath(
                                    Path().apply { p.first().let { pi -> moveTo(pi.first, pi.second) }; p.drop(1).forEachIndexed { idx, pi -> if (idx == 0) moveTo(pi.first, pi.second) else lineTo(pi.first, pi.second) } },
                                    Paint().apply { color = Color.White; strokeWidth = 2f; style = Paint.Style.Stroke }
                                )
                            }
                        }
                        if (currentPath.value?.size >= 2) {
                            graphically.drawPath(
                                Path().apply { currentPath.value.first().let { pi -> moveTo(pi.first, pi.second) }; currentPath.value.drop(1).forEachIndexed { idx, pi -> if (idx == 0) moveTo(pi.first, pi.second) else lineTo(pi.first, pi.second) } },
                                Paint().apply { color = Color.White; strokeWidth = 2f; style = Paint.Style.Stroke }
                            )
                        }
                    }
                    androidx.compose.ui.platform.LocalPainter.current = painter
                }
            }

            // Tap tally + word + rating
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(text = "TODAY: ${tapCount} TAPS", style = MaterialTheme.typography.body1,
                    color = MonobeaconTheme.composableColorScheme.onSurface, modifier = Modifier.weight(1f))

                OutlinedTextField(value = dailyWord, onValueChange = { dailyWord = it },
                    label = { Text(text = "How are you?", color = MonobeaconTheme.composableColorScheme.secondary) },
                    colors = OutlinedTextFieldColors(defaultColor = MonobeaconTheme.composableColorScheme.surface), modifier = Modifier.size(120.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(text = "RATING:", style = MaterialTheme.typography.body1,
                        color = MonobeaconTheme.composableColorScheme.onSurface)
                }
            }
        }

        // Bottom summary
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = "TODAY: ${tapCount} TAPS", style = MaterialTheme.typography.body1,
                color = MonobeaconTheme.composableColorScheme.onSurface, modifier = Modifier.weight(1f))

            OutlinedTextField(value = dailyWord, onValueChange = { dailyWord = it },
                label = { Text(text = "How are you?", color = MonobeaconTheme.composableColorScheme.secondary) },
                colors = OutlinedTextFieldColors(defaultColor = MonobeaconTheme.composableColorScheme.surface), modifier = Modifier.size(120.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(text = "RATING:", style = MaterialTheme.typography.body1, color = MonobeaconTheme.composableColorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    val ratings = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    for (rating in ratings) {
                        OutlinedTextField(value = if (dayRating == rating) rating.toString() else "",
                            onValueChange = { dayRating = rating }, label = { Text(text = "", visible = false) },
                                editable = false,
                                colors = OutlinedTextFieldColors(defaultColor = Color.Transparent, focusedColor = Color.Transparent),
                                modifier = Modifier.size(24.dp, 24.dp).background(
                                    if (dayRating >= rating) MonobeaconTheme.composableColorScheme.onSurface else Color.Transparent,
                                    shape = androidx.compose.ui.shape.CircleShape
                                )
                    )
                    }
                }
            }
        }
    }
}
