package com.monobeacon.compose.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialIconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Disposal
import androidx.compose.runtime.Recomposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusRequester
import androidx.compose.ui.unit.px
import androidx.compose.foundation.interaction.MemoryTaxonomy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumnItem
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.listState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.monobeacon.theme.MonobeaconTheme
import com.monobeacon.compose.animation.animateScrollFadeIn

// Screen destinations
enum class Destination {
    HOME(R.drawable.ic_home, "Home"),
    SETTINGS(R.drawable.ic_settings, "Settings");

    val iconRes: Int
    val label: String

    init {
        this.iconRes = if (name == "HOME") R.drawable.ic_home else R.drawable.ic_settings
        this.label = if (name == "HOME") "Home" else "Settings"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

// Scaffold with bottom navigation
@Composable
fun AppScaffold(
    selectedDestination: androidx.compose.runtime.State<Destination>,
    onDestinationSelected: (Destination) -> Unit
) {
    var host by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState { host = it }
            )
    ) {
        // Scroll reveal content
        ScreenContent(host = host)
        
        // Bottom navigation
        BottomNavigation(
            selectedIndex = selectedDestination.index,
            onSelect = { index ->
                onDestinationSelected(Destination.values()[index])
            }
        ) {
            Destination.HOME.name to {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.monobeacon.theme.MonobeaconTheme.contentColor
                ) {
                    HomeScreen(
                        onNavigateToSettings = { onDestinationSelected(Destination.SETTINGS) }
                    )
                }
            }
            Destination.SETTINGS.name to {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.monobeacon.theme.MonobeaconTheme.contentColor
                ) {
                    SettingsScreen(
                        onNavigateToHome = { onDestinationSelected(Destination.HOME) }
                    )
                }
            }
        }
    }
}

// Main screen content with scroll fade-in
@Composable
fun ScreenContent(host: Int) {
    val listState = rememberLazyListState()
    
    // Apply scroll fade-in to the column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState { host = it })
            .animateScrollFadeIn(listState = listState, index = 0),
        arrangement = Arrangement.Top,
        verticalScroll = rememberScrollState()
    ) {
        items(count = 3) { index ->
            // Apply scroll fade-in modifier to each item
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(width = androidx.compose.ui.half dimensions.dp, height = 200.dp)
                    .background(
                        color = androidx.compose.ui.graphics.Color.OffBlack,
                        shape = androidx.compose.ui.shape.RoundedCornerShape(16.dp)
                    )
                    // Apply the scrollFadeIn modifier
                    .animateScrollFadeIn(listState = listState, index = index)
            ) {
                // Content inside each card
                Center(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "Section ${index + 1}",
                        style = androidx.compose.material3.typography.h4,
                        color = androidx.compose.ui.graphics.Color.PureWhite
                    )
                }
            }
        }
    }
}

// Home screen composable
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit
) {
    val listState = rememberLazyListState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
            .verticalScroll(rememberScrollState()) { host ->
                // host can be used for tracking if needed
            },
        arrangement = Arrangement.Center,
        verticalScroll = rememberScrollState()
    ) {
        // Welcome/Optimization banner
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, bottom = 16.dp)
                .size(width = androidx.compose.ui.half dimensions.dp, height = 120.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color.OffBlack,
                    shape = androidx.compose.ui.shape.RoundedCornerShape(16.dp)
                )
                .animateScrollFadeIn(listState = listState, index = 0)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = androidx.compose.ui.Alignment.Horizontal.Center
            ) {
                androidx.compose.material3.Text(
                    text = "MONO // BEACON",
                    style = androidx.compose.material3.typography.h2,
                    color = androidx.compose.ui.graphics.Color.PureWhite,
                    fontWeight = androidx.compose.ui.font.FontWeight.Normal
                )
                androidx.compose.material3.Text(
                    text = "Locate your phone buried in bedding",
                    style = androidx.compose.material3.typography.body1,
                    color = androidx.compose.ui.graphics.Color.LightGray
                )
            }
        }
        
        // Quick Start Card
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .size(width = androidx.compose.ui.half dimensions.dp, height = 160.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color.OffBlack,
                    shape = androidx.compose.ui.shape.RoundedCornerShape(16.dp)
                )
                .animateScrollFadeIn(listState = listState, index = 1)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = androidx.compose.ui.Alignment.Horizontal.Center,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material3 Icons.Filled.Power,
                    androidx.compose.ui.graphics.Color.PureWhite,
                    contentDescription = "Power",
                    modifier = Modifier.size(48.dp)
                )
                androidx.compose.material3.Text(
                    text = "TRIGGER ALARM",
                    style = androidx.compose.material3.typography.h5,
                    color = androidx.compose.ui.graphics.Color.PureWhite
                )
                androidx.compose.material3.Text(
                    text = "Max volume, flashlight, vibration",
                    style = androidx.compose.material3.typography.body2,
                    color = androidx.compose.ui.graphics.Color.LightGray
                )
                androidx.compose.material3.Button(
                    onClick = { /* TODO: Trigger alarm */ },
                    style = androidx.compose.material3.ButtonStyle.Filled,
                    colors = androidx.compose.material3.ButtonDefaults.filledButtonColors(
                        backgroundColor = androidx.compose.ui.graphics.Color.PureWhite,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    androidx.compose.material3.Text(
                        text = "START",
                        style = androidx.compose.material3.typography.button
                    )
                }
            }
        }
        
        // Settings navigation
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .size(width = androidx.compose.ui.half dimensions.dp, height = 120.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color.OffBlack,
                    shape = androidx.compose.ui.shape.RoundedCornerShape(16.dp)
                )
                .animateScrollFadeIn(listState = listState, index = 2)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = androidx.compose.ui.Alignment.Horizontal.Center,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material3 Icons.Filled.Settings,
                    androidx.compose.ui.graphics.Color.LightGray,
                    contentDescription = "Settings",
                    modifier = Modifier.size(48.dp)
                )
                androidx.compose.material3.Text(
                    text = "SETTINGS",
                    style = androidx.compose.material3.typography.h5,
                    color = androidx.compose.ui.graphics.Color.LightGray
                )
            }
        }
    }
}

// Settings screen composable
@Composable
fun SettingsScreen(
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
        arrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Start
    ) {
        androidx.compose.material3.Text(
            text = "SETTINGS",
            style = androidx.compose.material3.typography.h2,
            color = androidx.compose.ui.graphics.Color.PureWhite,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Listening Mode Toggle
        SettingsToggleCard(
            title = "Listening Mode",
            options = [
                "Scheduled Window (08:00 AM - 11:00 PM)",
                "24/7 Always Active",
                "Paused"
            ],
            selectedIndex = 0,
            onSelect = { /* TODO: Handle selection */ }
        )
        
        // Custom keyword
        SettingsTextFieldCard(
            title = "Custom Secret SMS Keyword",
            initialValue = "RINGNOW",
            onValueChange = { /* TODO: Handle change */ }
        )
        
        // Alarm duration and flash
        SettingsToggleCard(
            title = "Alarm Duration",
            options = ["1 min", "2 mins (default)", "5 min", "10 min"],
            selectedIndex = 1,
            onSelect = { /* TODO: Handle selection */ }
        )
        
        SettingsToggleCard(
            title = "LED Strobe",
            options = ["Off", "On"],
            selectedIndex = 0,
            onSelect = { /* TODO: Handle selection */ }
        )
        
        // POCO/HyperOS setup helper
        SetupHelperCard()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Logout / Exit button
        androidx.compose.material3.Button(
            onClick = { onNavigateToHome() },
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = androidx.compose.ui.graphics.Color.LightGray
            )
        ) {
            androidx.compose.material3.Text(
                text = "BACK TO HOME",
                style = androidx.compose.material3.typography.button
            )
        }
    }
}

@Composable
fun SettingsToggleCard(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    androidx.compose.material3.OutlinedTextField(
        value = if (selectedIndex < options.size) options[selectedIndex] else options.first(),
        onValueChange = { /* TODO */ },
        label = { androidx.compose.material3.Text(text = title, color = androidx.compose.ui.graphics.Color.PureWhite) },
        colors = androidx.compose.material3.TextFieldColors(
            defaultColor = androidx.compose.ui.graphics.Color.OffBlack,
            focusedColor = androidx.compose.ui.graphics.Color.PureWhite,
            errorColor = androidx.compose.ui.graphics.Color.Red
        ),
        modifier = Modifier.fillMaxWidth().padding(4.dp)
    )
    androidx.compose.material3.ButtonButton(
        onClick = { onSelect(selectedIndex) },
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.PureWhite
        )
    ) {
        androidx.compose.material3.Text(
            text = "SELECT",
            style = androidx.compose.material3.typography.button
        )
    }
}

@Composable
fun SettingsTextFieldCard(
    title: String,
    initialValue: String,
    onValueChange: (String) -> Unit
) {
    androidx.compose.material3.OutlinedTextField(
        value = initialValue,
        onValueChange = onValueChange,
        label = { androidx.compose.material3.Text(text = title, color = androidx.compose.ui.graphics.Color.PureWhite) },
        colors = androidx.compose.material3.TextFieldColors(
            defaultColor = androidx.compose.ui.graphics.Color.OffBlack,
            focusedColor = androidx.compose.ui.graphics.Color.PureWhite
        ),
        modifier = Modifier.fillMaxWidth().padding(4.dp)
    )
}

@Composable
fun SetupHelperCard() {
    androidx.compose.material3.OutlinedButton(
        onClick = { /* TODO: Open POCO Autostart settings */ },
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.PureWhite
        )
    ) {
        androidx.compose.material3.Text(
            text = "POCO / HyperOS: Set Autostart & No Restrictions",
            style = androidx.compose.material3.typography.button
        )
    }
}
