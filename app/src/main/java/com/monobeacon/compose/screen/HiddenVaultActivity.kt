package com.monobeacon.compose.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import com.monobeacon.theme.MonobeaconTheme
import com.monobeacon.R

/**
 * HiddenVaultActivity - The real vault behind PIN gate
 * Shows: battery monitor, sticky notes, tap tally, sketchpad, auto-burn
 * Auto-closes when app goes to background (lifecycle observer in MainActivity)
 */
class HiddenVaultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply monochrome theme
        setTheme(R.style.AlertDialogTheme)

        setContent {
            MonobeaconTheme {
                // Apply system UI flags for immersive B&W mode
                androidx.compose.ui.platform.LocalView.currentView?.let { view ->
                    view.systemUiVisibility = androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_FULLSCREEN
                }

                setContent {
                    // Pass partnerCode via intent
                    val partnerCode = intent.getStringExtra("partnerCode") ?: "UNKNOWN"
                    HiddenVaultScreen(
                        onCloseVault = { finish() },
                        partnerCode = partnerCode
                    )
                }
            }
        }
    }
}
