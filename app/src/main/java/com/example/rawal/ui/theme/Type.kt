package com.example.rawal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.rawal.domain.i18n.AppLanguage

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.SemiBold),
    Font(R.font.vazirmatn, FontWeight.Bold)
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

fun buildRawalTypography(language: AppLanguage, fontSizeScaleKey: String): Typography {
    val family = if (language == AppLanguage.FA) VazirmatnFontFamily else PlusJakartaFontFamily
    val scale = when (fontSizeScaleKey) {
        "SMALL" -> 0.9f
        "LARGE" -> 1.14f
        else -> 1.0f
    }

    return Typography(
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (32f * scale).sp,
            lineHeight = (40f * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * scale).sp,
            lineHeight = (32f * scale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * scale).sp,
            lineHeight = (28f * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * scale).sp,
            lineHeight = (26f * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * scale).sp,
            lineHeight = (18f * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * scale).sp,
            lineHeight = (16f * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * scale).sp,
            lineHeight = (16f * scale).sp
        )
    )
}
