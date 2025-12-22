package com.lsj.mp7.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lsj.mp7.ui.screens.AppTheme
import com.lsj.mp7.ui.screens.ThemeState

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF666666),
    // In Light mode use dark borders so elements are clearly separated
    outline = Color.Black,
    outlineVariant = Color(0xFF555555)
)

private val OLEDColorScheme = darkColorScheme(
    primary = OLEDPrimary,
    secondary = OLEDSecondary,
    tertiary = OLEDTertiary,
    background = OLEDBackground,
    surface = OLEDSurface,
    surfaceVariant = Color(0xFF000000),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB3B3B3),
    // In OLED mode use light borders so elements stand out on pure black
    outline = Color.White,
    outlineVariant = Color(0xFFAAAAAA)
)

@Composable
fun Mp7Theme(
    appTheme: AppTheme = ThemeState.currentTheme,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to use our custom themes
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.Light -> LightColorScheme
        AppTheme.Dark -> {
            // Use dynamic color for Dark theme if available and enabled
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
                dynamicDarkColorScheme(context).copy(
                    background = Color(0xFF121212),
                    surface = Color(0xFF121212)
                )
            } else {
                DarkColorScheme
            }
        }
        AppTheme.OLED -> OLEDColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}