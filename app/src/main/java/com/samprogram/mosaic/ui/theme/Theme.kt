package com.samprogram.mosaic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Initial theme values mapped from the semantic color tokens in `ui-context.md`.
// They are intentionally centralized here so feature composables never need to
// hardcode colors.

private val MosaicLightColors = lightColorScheme(
    primary = Color(0xFFFF4500),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFDAE0E6),
    onBackground = Color(0xFF1A1A1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1B),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
)

private val MosaicDarkColors = darkColorScheme(
    primary = Color(0xFFFF4500),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFF0E1113),
    onBackground = Color(0xFFF2F4F5),
    surface = Color(0xFF181C1F),
    onSurface = Color(0xFFF2F4F5),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF1A1A1B),
)

@Composable
fun MosaicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MosaicDarkColors else MosaicLightColors,
        content = content,
    )
}
