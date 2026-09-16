package com.cimdriver.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CIMDriverNavy,
    onPrimary = CIMDriverWhite,
    secondary = CIMDriverGreen,
    onSecondary = CIMDriverWhite,
    tertiary = CIMDriverGreenLight,
    background = CIMDriverGray,
    surface = CIMDriverWhite,
    onBackground = CIMDriverNavy,
    onSurface = CIMDriverNavy
)

private val DarkColors = darkColorScheme(
    primary = CIMDriverGreenLight, // Vibrant green stands out beautifully on dark backgrounds
    onPrimary = CIMDriverNavyDark, // Dark text inside green buttons
    primaryContainer = CIMDriverGreen,
    onPrimaryContainer = CIMDriverWhite,
    secondary = CIMDriverBluePastel,
    onSecondary = CIMDriverNavyDark,
    secondaryContainer = CIMDriverNavyLight,
    onSecondaryContainer = CIMDriverWhite,
    background = Color(0xFF121212), // Deep dark/OLED-friendly background for high contrast
    onBackground = Color(0xFFFFFFFF), // Crisp white text
    surface = Color(0xFF1E1E24), // Slightly lighter surface for cards
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2A2A32),
    onSurfaceVariant = Color(0xFFD0D4DC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun CIMDriverTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (useDarkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
