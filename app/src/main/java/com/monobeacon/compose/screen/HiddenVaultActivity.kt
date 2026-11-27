package com.monobeacon.compose.screen

import com.monobeacon.monobeacon.R

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.textUnit
import com.monobeacon.theme.MonobeaconTheme
import com.monobeacon.R

/**
 * HiddenVaultActivity - The real vault behind PIN gate
 * Shows: battery monitor, sticky notes, tap tally, sketchpad, auto-burn
 * Auto-closes when app goes to background (lifecycle observer)
 */
class HiddenVaultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply monochrome theme
        setTheme(R.style.AlertDialogTheme)

        setContent {
            MonobeaconTheme {
                // Apply system UI dark mode for full B&W
                SystemUiFlagModifier.applySystemUiFlags(
                    this,
                    androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_FULLSCREEN
                )

                setContent {
                    HiddenVaultScreen(
                        onCloseVault = { finish() },
                        partnerCode = intent.getStringExtra("partnerCode") ?: "UNKNOWN"
                    )
                }
            }
        }
    }
}

/**
 * SystemDiagnosticsActivity - Decoy screen for wrong PIN
 * Shows realistic monochrome hardware logs to deter snoopers
 */
class SystemDiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AlertDialogTheme)

        setContent {
            MonobeaconTheme {
                SystemUiFlagModifier.applySystemUiFlags(
                    this,
                    androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

                setContent {
                    SystemDiagnosticsScreen(
                        onReturnHome = { finish() }
                    )
                }
            }
        }
    }
}

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

/**
 * SystemUiFlagModifier - Helper for system UI flags
 */
object SystemUiFlagModifier {
    @Composable
    fun applySystemUiFlags(
        activity: androidx.appcompat.app.AppCompatActivity,
        flags: Int
    ) {
        androidx.compose.ui.platform.LocalView.currentView?.let { view ->
            view.systemsUiFlags = flags
        }
    }
}
