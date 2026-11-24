package com.monobeacon.compose.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import com.monobeacon.theme.MonobeaconTheme
import com.monobeacon.R

/**
 * AlarmActiveActivity - Full-screen activity shown over lock screen
 * Features stark black-and-white full-screen pulsing border and massive "STOP ALARM" button
 * Launched from AlarmService when triggered
 */
class AlarmActiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply monochrome theme
        setTheme(R.style.AlertDialogTheme)

        setContentView(R.layout.activity_alarm_active)
        
        // Ensure activity stays on screen and over lock screen
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        // Set lock screen visibility
        window.setType(
            android.view.WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG
        )
    }
}

/**
 * Composable for the Alarm Active UI shown over lock screen
 * Stark black-and-white full-screen pulsing border with massive STOP ALARM button
 */
@Composable
fun AlarmActiveUI(
    onStopAlarm: () -> Unit,
    context: androidx.compose.ui.context.Context
) {
    val appContext = LocalContext.current
    
    // Pulsing border animation state
    var pulseProgress by remember { mutableStateOf(0f) }
    
    // Animate the pulsing effect
    val pulseSpec = androidx.compose.animation.tween(2000, androidx.compose.animation.FastOutSlowInEasing)
    
    rememberAnimatable(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = pulseSpec,
        iterationCount = androidx.compose.animation.InfiniteRepeatable(1)
    ) { value ->
        pulseProgress = value
    }
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Color.Transparent
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .alignment(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsing black border effect
            Box(
                modifier = Modifier
                    .size(300.dp)  // Large square
                    .background(
                        color = androidx.compose.ui.graphics.Color.OffBlack,
                        shape = androidx.compose.ui.shape.RoundedCornerShape(24.dp)
                    )
                    // Pulse effect via elevation/inset animation
                    .offset { IntOffset(
                        x = -((300.dp * pulseProgress / 2).roundToInt()),
                        y = -((300.dp * pulseProgress / 2).roundToInt())
                    )}
            ) {
                // Inner white content area
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color.PureWhite,
                            shape = androidx.compose.ui.shape.RoundedCornerShape(20.dp)
                        )
                )
            }
            
            // Emergency STOP ALARM button - massive and prominent
            Button(
                modifier = Modifier
                    .width(300.dp)
                    .height(80.dp)
                    .background(
                        color = androidx.compose.ui.graphics.Color.Red,
                        shape = androidx.compose.ui.shape.RoundedCornerShape(16.dp)
                    ),
                onClick = {
                    onStopAlarm()
                }
            ) {
                Text(
                    text = "STOP ALARM",
                    style = androidx.compose.material3.typography.h4,
                    color = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Status text
            Text(
                text = "ALARM ACTIVE - Phone is lost",
                style = androidx.compose.material3.typography.h6,
                color = androidx.compose.ui.graphics.Color.Black,
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.textAlign.Center
            )
            
            // Instructions
            Text(
                text = "Triggered from partner's phone\nor via SMS command",
                style = androidx.compose.material3.typography.body2,
                color = androidx.compose.ui.graphics.Color.Charcoal,
                textAlign = androidx.compose.ui.textAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
