package com.perfectapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PerfectColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF70DCC5),
    onPrimary = BackgroundDark,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF244D46),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFBCF0E3),
    secondary = androidx.compose.ui.graphics.Color(0xFFB8B4FF),
    onSecondary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = NegativeRed
)

@Composable
fun PerfectAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PerfectColorScheme else lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF006B5C),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = androidx.compose.ui.graphics.Color(0xFFBCF0E3),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF00382F),
            secondary = androidx.compose.ui.graphics.Color(0xFF6255A4),
            background = androidx.compose.ui.graphics.Color(0xFFF3F7F6),
            surface = androidx.compose.ui.graphics.Color.White,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE7EFEC),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF50635D),
            outline = androidx.compose.ui.graphics.Color(0xFFD4E1DC)
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
