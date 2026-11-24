package com.monobeacon.theme

import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDarkTheme
import androidx.compose.ui.platform.LocalView

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

private val MonochromeLightColorScheme = lightColorScheme(
    brightness = androidx.compose.ui.graphics.Brightness.Light,
    primary = Color(0xFF000000),    // Black
    onPrimary = Color(0xFFFFFFFF),  // White text
    secondary = Color(0xFF333333),  // Dark Charcoal
    onSecondary = Color(0xFFFFFFFF), // White on secondary
    surface = Color(0xFFFFFFFF),     // White
    onSurface = Color(0xFF000000),   // Black on surface
    error = Color(0xFFFFFFFF),
    onError = Color(0xFF000000),
    background = Color(0xFFFFFFFF),  // White
    onBackground = Color(0xFF000000) // Black on background
)

// Monochrome theme wrapper
fun MonobeaconTheme(darkTheme: Boolean = isSystemInDarkMode(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// Typographic hierarchy - clean, minimalist geometric/editorial sans-serif
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
    subtitle1 = TextStyle(
        fontFamily = "Roboto",
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.5.sp
    ),
    subtitle2 = TextStyle(
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

// Component styles
object Styles {
    val MonochromeTextButton = androidx.compose.material3.TextButtonStyle(
        backgroundColor = Color.Transparent,
        contentColor = androidx.compose.ui.graphics.Color.PureWhite,
        hoverColor = Color(0x1AFFFFFF),
        enabledBackgroundColor = Color.Transparent,
        enabledContentColor = androidx.compose.ui.graphics.Color.PureWhite,
        pressedBackgroundColor = Color(0x1AFFFFFF)
    )
    
    val MonochromeFilledButton = androidx.compose.material3.ButtonStyle(
        backgroundColor = androidx.compose.ui.graphics.Color.PureWhite,
        contentColor = Color(0xFF000000),
        hoverColor = Color(0x1A000000),
        enabledBackgroundColor = androidx.compose.ui.graphics.Color.PureWhite,
        enabledContentColor = Color(0xFF000000),
        pressedBackgroundColor = Color(0x1A000000),
        textStyle = Typography.button
    )
    
    val MonochromeOutlinedButton = androidx.compose.material3.ButtonStyle(
        backgroundColor = Color.Transparent,
        contentColor = androidx.compose.ui.graphics.Color.PureWhite,
        borderColor = androidx.compose.ui.graphics.Color.HairlineBorder,
        hoverColor = Color(0x1AFFFFFF),
        enabledBackgroundColor = Color.Transparent,
        enabledContentColor = androidx.compose.ui.graphics.Color.PureWhite,
        pressedBackgroundColor = Color(0x1AFFFFFF),
        textStyle = Typography.button
    )
}
