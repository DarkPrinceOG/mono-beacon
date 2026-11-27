package com.monobeacon.theme

import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDarkTheme

// Strict monochrome color scheme
private val MonochromeDarkColorScheme = lightColorScheme(
    brightness = androidx.compose.ui.graphics.Brightness.Dark,
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF8A8A8A),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFFFFFFF),
    error = Color(0xFFFFFFFF),
    onError = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF)
)

private val MonochromeLightColorScheme = lightColorScheme(
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
)

// Monochrome theme wrapper
@Composable
fun MonobeaconTheme(darkTheme: Boolean = isSystemInDarkMode(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

val Typography = typography2024().copy(
    h1 = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Bold, fontSize = 32.sp, letterSpacing = 1.5.sp),
    h2 = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Bold, fontSize = 24.sp, letterSpacing = 1.5.sp),
    h3 = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.5.sp),
    h4 = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.5.sp),
    body1 = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.5.sp),
    body2 = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.5.sp),
    button = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.5.sp),
    overline = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.font.FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.0.sp)
)

val Shapes = androidx.compose.ui.shape.MaterialShapes(
    small = androidx.compose.ui.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.ui.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.ui.shape.RoundedCornerShape(16.dp)
)
