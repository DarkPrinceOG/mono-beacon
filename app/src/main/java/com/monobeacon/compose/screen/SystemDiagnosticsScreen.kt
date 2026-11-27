package com.monobeacon.compose.screen

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.monobeacon.theme.MonobeaconTheme

/**
 * SystemDiagnosticsScreen - Decoy screen shown when wrong PIN entered
 * Displays realistic monochrome hardware logs to deter snoopers
 */
@Composable
fun SystemDiagnosticsScreen(
    onReturnHome: () -> Unit
) {
    // Static decoy values - doesn't actually read system properties
    val batteryHealth = "GOOD"
    val radioState = "IDLE"
    val wakeLocks = "0"
    val chargeCycles = "847"

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

        // Battery health card
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(80.dp)
                .background(backgroundColor = Color.OffBlack)
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
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "CHARGE CYCLES: $chargeCycles",
                    style = MaterialTheme.typography.caption,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Radio state card
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(80.dp)
                .background(backgroundColor = Color.OffBlack)
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
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "SIGNAL STRENGTH: Good",
                    style = MaterialTheme.typography.caption,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Wake locks card
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(80.dp)
                .background(backgroundColor = Color.OffBlack)
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
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PARTIAL_WAKE_LOCK: false",
                    style = MaterialTheme.typography.caption,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Uptime card
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(80.dp)
                .background(backgroundColor = Color.OffBlack)
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
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "BOOT_TIME: Simulated",
                    style = MaterialTheme.typography.caption,
                    color = Color.LightGray,
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
                contentColor = Color.White
            )
        ) {
            Text(
                text = "RETURN TO HOME",
                style = MaterialTheme.typography.button,
                color = Color.White
            )
        }
    }
}
