package com.example.rawal.ui.theme

import androidx.compose.ui.graphics.Color

enum class AccentPalette(
    val key: String,
    val labelEn: String,
    val labelFa: String,
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val secondaryLight: Color,
    val primaryDark: Color,
    val primaryContainerDark: Color
) {
    TEAL(
        key = "TEAL",
        labelEn = "Deep Teal",
        labelFa = "سبز آبی",
        primaryLight = Color(0xFF0F766E),
        primaryContainerLight = Color(0xFFCCFBF1),
        onPrimaryContainerLight = Color(0xFF134E4A),
        secondaryLight = Color(0xFF0D9488),
        primaryDark = Color(0xFF5EEAD4),
        primaryContainerDark = Color(0xFF115E59)
    ),
    INDIGO(
        key = "INDIGO",
        labelEn = "Royal Indigo",
        labelFa = "نیلی",
        primaryLight = Color(0xFF4338CA),
        primaryContainerLight = Color(0xFFE0E7FF),
        onPrimaryContainerLight = Color(0xFF312E81),
        secondaryLight = Color(0xFF4F46E5),
        primaryDark = Color(0xFFA5B4FC),
        primaryContainerDark = Color(0xFF3730A3)
    ),
    EMERALD(
        key = "EMERALD",
        labelEn = "Emerald",
        labelFa = "زمردی",
        primaryLight = Color(0xFF047857),
        primaryContainerLight = Color(0xFFD1FAE5),
        onPrimaryContainerLight = Color(0xFF064E3B),
        secondaryLight = Color(0xFF059669),
        primaryDark = Color(0xFF6EE7B7),
        primaryContainerDark = Color(0xFF065F46)
    ),
    AMBER(
        key = "AMBER",
        labelEn = "Warm Amber",
        labelFa = "کهربایی",
        primaryLight = Color(0xFFB45309),
        primaryContainerLight = Color(0xFFFEF3C7),
        onPrimaryContainerLight = Color(0xFF78350F),
        secondaryLight = Color(0xFFD97706),
        primaryDark = Color(0xFFFCD34D),
        primaryContainerDark = Color(0xFF92400E)
    ),
    ROSE(
        key = "ROSE",
        labelEn = "Calm Rose",
        labelFa = "رز",
        primaryLight = Color(0xFFBE123C),
        primaryContainerLight = Color(0xFFFFE4E6),
        onPrimaryContainerLight = Color(0xFF881337),
        secondaryLight = Color(0xFFE11D48),
        primaryDark = Color(0xFFFDA4AF),
        primaryContainerDark = Color(0xFF9F1239)
    ),
    SLATE(
        key = "SLATE",
        labelEn = "Nordic Slate",
        labelFa = "خاکستری آبی",
        primaryLight = Color(0xFF334155),
        primaryContainerLight = Color(0xFFE2E8F0),
        onPrimaryContainerLight = Color(0xFF0F172A),
        secondaryLight = Color(0xFF475569),
        primaryDark = Color(0xFFCBD5E1),
        primaryContainerDark = Color(0xFF1E293B)
    );

    companion object {
        fun fromKey(key: String?): AccentPalette =
            entries.firstOrNull { it.key == key } ?: TEAL
    }
}

val RawalLightBackground = Color(0xFFF8FAFC)
val RawalLightSurface = Color(0xFFFFFFFF)
val RawalLightSurfaceVariant = Color(0xFFF1F5F9)
val RawalLightOutline = Color(0xFFCBD5E1)
val RawalLightOutlineVariant = Color(0xFFE2E8F0)
val RawalLightTextPrimary = Color(0xFF0F172A)
val RawalLightTextSecondary = Color(0xFF475569)

val RawalDarkBackground = Color(0xFF0B1117)
val RawalDarkSurface = Color(0xFF111922)
val RawalDarkSurfaceVariant = Color(0xFF1B2633)
val RawalDarkOutline = Color(0xFF334155)
val RawalDarkOutlineVariant = Color(0xFF243242)
val RawalDarkTextPrimary = Color(0xFFF1F5F9)
val RawalDarkTextSecondary = Color(0xFF94A3B8)

val ScoreHighColor = Color(0xFF059669)
val ScoreMediumColor = Color(0xFFD97706)
val ScoreLowColor = Color(0xFFE11D48)

val CategoryColorPresets = listOf(
    0xFF4F46E5L,
    0xFF0F766EL,
    0xFF059669L,
    0xFFD97706L,
    0xFF0891B2L,
    0xFFE11D48L,
    0xFF7C3AEDL,
    0xFF475569L,
    0xFFDB2777L,
    0xFF2563EBL
)
