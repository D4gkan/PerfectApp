package com.perfectapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PerfectColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFFB3B8),
    onPrimary = BackgroundDark,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF691923),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDADD),
    secondary = androidx.compose.ui.graphics.Color(0xFFC7C7CE),
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    surfaceTint = androidx.compose.ui.graphics.Color(0xFFFFB3B8),
    error = NegativeRed
)

@Composable
fun PerfectAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PerfectColorScheme else lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF9B111E),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDADD),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF400009),
            secondary = androidx.compose.ui.graphics.Color(0xFF5D5D67),
            background = androidx.compose.ui.graphics.Color(0xFFF7F7F9),
            surface = androidx.compose.ui.graphics.Color.White,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEBEBEF),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF55555F),
            outline = androidx.compose.ui.graphics.Color(0xFFC7C7CF)
        ),
        typography = PerfectTypography,
        content = {
            androidx.compose.material3.Surface(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                content = content
            )
        }
    )
}
