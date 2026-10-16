package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Liquid Glass dark color scheme ───────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = CinemaRed,
    onPrimary            = TextPrimary,
    primaryContainer     = CinemaRedLight,
    onPrimaryContainer   = TextPrimary,
    secondary            = CinemaAmber,
    onSecondary          = CinemaBlack,
    secondaryContainer   = CinemaSurfaceVariant,
    onSecondaryContainer = CinemaAmber,
    tertiary             = CinemaCyan,
    onTertiary           = CinemaBlack,
    background           = CinemaBlack,
    onBackground         = TextPrimary,
    surface              = CinemaDark,
    onSurface            = TextPrimary,
    surfaceVariant       = CinemaSurface,
    onSurfaceVariant     = TextSecondary,
    outline              = CinemaBorder,
    outlineVariant       = CinemaBorder
)

/**
 * Root theme for CinePulse.
 *
 * Enables full edge-to-edge rendering so the liquid glass navigation bar
 * and hero banner can bleed behind system UI chrome.  Status bar and
 * navigation bar are forced transparent with dark icon appearance.
 */
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent system bars – glass nav bar draws its own scrim
            @Suppress("DEPRECATION")
            window.statusBarColor     = 0x00000000   // Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = 0x00000000   // Color.TRANSPARENT
            // Let the app layout extend behind the system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Keep icons light-on-dark for the cinema palette
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars     = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
