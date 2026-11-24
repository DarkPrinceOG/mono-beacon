package com.monobeacon.compose.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialButton
import androidx.compose.material3.MaterialTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.monobeacon.data.DataStoreManager
import com.monobeacon.data.PreferencesSchema
import com.monobeacon.theme.MonobeaconTheme

/**
 * SettingsActivity - On-phone settings for MONO // BEACON
 * Clean monochrome interface for customizing all beacon parameters
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MonobeaconTheme {
                Scaffold(
                    scaffoldState = rememberScaffoldState(),
                    bottomBar = {
                        BottomNavigation {
                            navigationBarItems(
                                bottomNavItem(
                                    icon = { Icon(
                                        imageVector = androidx.compose.material3.Icons.Filled.Home,
                                        contentDescription = "Home"
                                    ) },
                                    label = { Text("Home") }
                                ),
                                bottomNavItem(
                                    icon = { Icon(
                                        imageVector = androidx.compose.material3.Icons.Filled.Settings,
                                        contentDescription = "Settings"
                                    ) },
                                    label = { Text("Settings") }
                                )
                            ) {
                                onSelectIndex = { index ->
                                    // Handle bottom nav selection if needed
                                }
                            }
                        }
                    }
                ) {
                    // Main settings content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        arrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Start
                    ) {
                        // --- Listening Mode Section ---
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(androidx.compose.ui.graphics.Color.OffBlack)
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "LISTENING MODE",
                                style = androidx.compose.material3.typography.h6,
                                color = androidx.compose.ui.graphics.Color.LightGray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val (listeningMode, setListeningMode) = remember { mutableStateOf("scheduled") }
                            
                            RadioGroup(
                                selectedValue = listeningMode,
                                onValueSelected = { setListeningMode(it) }
                            ) {
                                RadioItem("Scheduled Window") {
                                    setListeningMode("scheduled")
                                    // Save to DataStore
                                    DataStoreManager.setListeningMode(LocalContext.current, "scheduled")
                                }
                                RadioItem("24/7 Always Active") {
                                    setListeningMode("24/7")
                                    DataStoreManager.setListeningMode(LocalContext.current, "24/7")
                                }
                                RadioItem("Paused") {
                                    setListeningMode("paused")
                                    DataStoreManager.setListeningMode(LocalContext.current, "paused")
                                }
                            }
                        }
                        
                        // --- Custom SMS Keyword ---
                        SettingsTextFieldCard(
                            title = "Custom Secret SMS Keyword",
                            initialValue = DataStoreManager.getSecretSmsKeyword(LocalContext.current),
                            onValueChange = { newValue ->
                                DataStoreManager.setSecretSmsKeyword(LocalContext.current, newValue)
                            }
                        )
                        
                        // --- Alarm Duration ---
                        SettingsToggleCard(
                            title = "Alarm Duration",
                            options = ["1 min", "2 mins (default)", "5 min", "10 min"],
                            selectedIndex = when {
                                DataStoreManager.getAlarmDuration(LocalContext.current) == 1 -> 0
                                DataStoreManager.getAlarmDuration(LocalContext.current) == 5 -> 2
                                DataStoreManager.getAlarmDuration(LocalContext.current) == 10 -> 3
                                else -> 1
                            },
                            onSelect = { index ->
                                val durations = mapOf(0 to 1, 1 to 2, 2 to 5, 3 to 10)
                                val selectedDuration = durations[index] ?: 2
                                DataStoreManager.setAlarmDuration(LocalContext.current, selectedDuration)
                            }
                        )
                        
                        // --- LED Strobe ---
                        SettingsToggleCard(
                            title = "LED Strobe",
                            options = ["Off", "On"],
                            selectedIndex = when {
                                DataStoreManager.isLedStrobeEnabled(LocalContext.current) -> 1
                                else -> 0
                            },
                            onSelect = { isEnabled ->
                                DataStoreManager.setLedStrobe(LocalContext.current, isEnabled)
                            }
                        )
                        
                        // --- POCO / HyperOS Setup ---
                        SetupHelperCard()
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // --- Save & Exit ---
                        Button(
                            onClick = {
                                // Save all settings and navigate back
                                onBackPressed()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.Transparent,
                                contentColor = androidx.compose.ui.graphics.Color.PureWhite
                            )
                        ) {
                            Text(
                                text = "SAVE AND EXIT",
                                style = androidx.compose.material3.typography.button,
                                color = androidx.compose.ui.graphics.Color.PureWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper composables reused from MainActivity

@Composable
fun RadioGroup(
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    content: @Composable (@Composable RowScope.() -> Unit) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        content()
    }
}

@Composable
fun RadioItem(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(
                if (/* selected */ true) androidx.compose.ui.graphics.Color.PureWhite else Color.Transparent,
                shape = androidx.compose.ui.shape.RoundedCornerShape(8.dp)
            ),
        onClick = onClick
    ) {
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.PureWhite,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun SettingsTextFieldCard(
    title: String,
    initialValue: String,
    onValueChange: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    
    OutlinedTextField(
        value = value,
        onValueChange = { value = it; onValueChange(it) },
        label = { Text(text = title, color = androidx.compose.ui.graphics.Color.PureWhite) },
        colors = androidx.compose.material3.TextFieldColors(
            defaultColor = androidx.compose.ui.graphics.Color.OffBlack,
            focusedColor = androidx.compose.ui.graphics.Color.PureWhite
        ),
        modifier = Modifier.fillMaxWidth().padding(4.dp)
    )
}

@Composable
fun SetupHelperCard() {
    OutlinedButton(
        onClick = {
            // Open POCO Autostart settings intent
            val intent = android.content.Intent("miui.intent.action.APP_PERM_EDITOR")
            if (intent.resolveActivity(LocalContext.current.packageManager) != null) {
                LocalContext.current.startActivity(intent)
            } else {
                // Fallback: open App Info
                val fallbackIntent = android.content.Intent(android.settings.APPLICATION_INFO_SETTINGS)
                fallbackIntent.data = android.net.Uri.parse("package:${LocalContext.current.packageName}")
                LocalContext.current.startActivity(fallbackIntent)
            }
        },
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.PureWhite
        )
    ) {
        Text(
            text = "POCO / HyperOS: Set Autostart & No Restrictions",
            style = androidx.compose.material3.typography.button
        )
    }
}
