package com.example.rawal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.rawal.domain.i18n.AppLanguage

private val RawalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun RawalTheme(
    language: AppLanguage = AppLanguage.EN,
    accentKey: String = "TEAL",
    fontSizeScale: String = "MEDIUM",
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val accent = AccentPalette.fromKey(accentKey)
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent.primaryDark,
            onPrimary = Color(0xFF062E2B),
            primaryContainer = accent.primaryContainerDark,
            onPrimaryContainer = Color(0xFFE2E8F0),
            secondary = accent.secondaryLight,
            onSecondary = Color.White,
            background = RawalDarkBackground,
            onBackground = RawalDarkTextPrimary,
            surface = RawalDarkSurface,
            onSurface = RawalDarkTextPrimary,
            surfaceVariant = RawalDarkSurfaceVariant,
            onSurfaceVariant = RawalDarkTextSecondary,
            outline = RawalDarkOutline,
            outlineVariant = RawalDarkOutlineVariant,
            error = Color(0xFFFCA5A5)
        )
    } else {
        lightColorScheme(
            primary = accent.primaryLight,
            onPrimary = Color.White,
            primaryContainer = accent.primaryContainerLight,
            onPrimaryContainer = accent.onPrimaryContainerLight,
            secondary = accent.secondaryLight,
            onSecondary = Color.White,
            background = RawalLightBackground,
            onBackground = RawalLightTextPrimary,
            surface = RawalLightSurface,
            onSurface = RawalLightTextPrimary,
            surfaceVariant = RawalLightSurfaceVariant,
            onSurfaceVariant = RawalLightTextSecondary,
            outline = RawalLightOutline,
            outlineVariant = RawalLightOutlineVariant,
            error = Color(0xFFDC2626)
        )
    }

    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = buildRawalTypography(language, fontSizeScale)

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = RawalShapes,
            content = content
        )
    }
}
