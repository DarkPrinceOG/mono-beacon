package com.monobeacon.theme

import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDarkTheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.monobeacon.compose.screen.HiddenVaultScreen
import com.monobeacon.compose.screen.MainActivity

// Color palette - strict monochrome
private val MonochromeDarkColorScheme = lightColorScheme(
    brightness = androidx.compose.ui.graphics.Brightness.Dark,
    primary = Color(0xFFFFFFFF),    // Pure White
    onPrimary = Color(0xFF000000),  // Black text
    secondary = Color(0xFF8A8A8A),  // Light Gray
    onSecondary = Color(0xFFFFFFFF), // White on secondary
    surface = Color(0xFF0D0D0D),     // Off-Black
    onSurface = Color(0xFFFFFFFF),   // White on surface
    error = Color(0xFFFFFFFF),
    onError = Color(0xFF000000),
    background = Color(0xFF000000),  // AMOLED Black
    onBackground = Color(0xFFFFFFFF) // White on background
)

// Monochrome theme wrapper - Strict B&W only
fun MonobeaconTheme(darkTheme: Boolean = isSystemInDarkMode(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) MonochromeDarkColorScheme else lightColorScheme(
            brightness = androidx.compose.ui.graphics.Brightness.Light,
            primary = Color(0xFF000000),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF333333),
            onSecondary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF000000),
            error = Color(0xFFFFFFFF),
            onError = Color(0xFF000000),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000)
        ),
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// Typographic hierarchy
val Typography = typography2024().copy(
    h1 = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 1.5.sp
    ),
    h2 = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 1.5.sp
    ),
    h3 = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.5.sp
    ),
    h4 = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.5.sp
    ),
    body1 = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    body2 = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
    button = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.5.sp
    ),
    overline = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 2.0.sp
    )
)

// Shapes - crisp, minimal
val Shapes = androidx.compose.ui.shape.MaterialShapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

// Component styles object
object Styles {
    val MonochromeTextButton = androidx.compose.material3.TextButtonStyle(
        backgroundColor = Color.Transparent,
        contentColor = Color.PureWhite,
        hoverColor = Color(0x1AFFFFFF),
        enabledBackgroundColor = Color.Transparent,
        enabledContentColor = Color.PureWhite,
        pressedBackgroundColor = Color(0x1AFFFFFF)
    )
    
    val MonochromeFilledButton = androidx.compose.material3.ButtonStyle(
        backgroundColor = Color.PureWhite,
        contentColor = Color(0xFF000000),
        hoverColor = Color(0x1A000000),
        enabledBackgroundColor = Color.PureWhite,
        enabledContentColor = Color(0xFF000000),
        pressedBackgroundColor = Color(0x1A000000),
        textStyle = Typography.button
    )
    
    val MonochromeOutlinedButton = androidx.compose.material3.ButtonStyle(
        backgroundColor = Color.Transparent,
        contentColor = Color.PureWhite,
        borderColor = Color.Hex("#262626"),
        hoverColor = Color(0x1AFFFFFF),
        enabledBackgroundColor = Color.Transparent,
        enabledContentColor = Color.PureWhite,
        pressedBackgroundColor = Color(0x1AFFFFFF),
        textStyle = Typography.button
    )
}

/**
 * Helper: Disguised Launcher Icon - Monochrome geometric radar/circle
 * This sets the adaptive icon mask to pure B&W
 */
@Composable
fun DisguisedLauncherIcon(
    contentDescription: String = "Beacon launcher",
    modifier: Modifier = Modifier
) {
    // The adaptive icon is set in AndroidManifest.xml
    // This composable provides the visual asset
    androidx.compose.material3.Icon(
        imageVector = Icons.Filled.Power, // Placeholder - actual icon from mipmap
        contentDescription = contentDescription,
        modifier = modifier
    )
}

/**
 * Applies system UI flags for full B&W immersive mode
 */
@Composable
fun applyImmersiveSystemUi(
    activity: androidx.appcompat.app.AppCompatActivity
) {
    val view = androidx.compose.ui.platform.LocalView.currentView
    if (view != null) {
        view.systemUiVisibility = androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_FULLSCREEN
            or androidx.core.view.ViewCompat.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
