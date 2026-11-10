package com.filldemaia.cronos.radio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filldemaia.cronos.R

// ---------------------------------------------------------------------------
// Paleta Egeu
// Blau nit profund, marbre clar, blau Egeu i daurat càlid.
// Es mantenen els noms històrics (Radio*) perquè tota la interfície existent
// els pugui seguir utilitzant sense canvis de risc.
// ---------------------------------------------------------------------------

val EgeuNavy = Color(0xFF071420)
val EgeuNavyDeep = Color(0xFF040C13)
val EgeuSurface = Color(0xFF0C1B28)
val EgeuSurfaceHigh = Color(0xFF132636)
val EgeuSurfaceHighest = Color(0xFF1A3346)
val EgeuMarble = Color(0xFFF4EFE5)
val EgeuMist = Color(0xFF9FB2C1)
val EgeuBlue = Color(0xFF3FA9D9)
val EgeuBlueDeep = Color(0xFF1E6A91)
val EgeuGold = Color(0xFFD9AD5B)
val EgeuGoldSoft = Color(0xFFF0CB86)
val EgeuGreen = Color(0xFF55D6A3)
val EgeuRed = Color(0xFFF47B75)
val EgeuOnAccent = Color(0xFF04121C)

val RadioOrange = EgeuBlue
val RadioOrangeDeep = EgeuBlueDeep
val RadioNight = EgeuNavy
val RadioSurface = EgeuSurface
val RadioSurfaceHigh = EgeuSurfaceHigh
val RadioSurfaceHighest = EgeuSurfaceHighest
val RadioText = EgeuMarble
val RadioMuted = EgeuMist
val RadioGreen = EgeuGreen
val RadioYellow = EgeuBlue
val RadioRed = EgeuRed
val RadioBlue = EgeuBlue
val RadioOnAccent = EgeuOnAccent

private val EgeuScheme = darkColorScheme(
    primary = EgeuBlue,
    onPrimary = EgeuOnAccent,
    primaryContainer = EgeuBlueDeep,
    onPrimaryContainer = Color(0xFFE7F6FF),
    secondary = EgeuBlue,
    onSecondary = EgeuOnAccent,
    secondaryContainer = Color(0xFF173D52),
    onSecondaryContainer = Color(0xFFD0EDFC),
    tertiary = EgeuGreen,
    onTertiary = EgeuOnAccent,
    background = EgeuNavy,
    onBackground = EgeuMarble,
    surface = EgeuSurface,
    onSurface = EgeuMarble,
    surfaceVariant = EgeuSurfaceHigh,
    onSurfaceVariant = EgeuMist,
    surfaceContainer = EgeuSurfaceHigh,
    surfaceContainerHigh = EgeuSurfaceHighest,
    error = EgeuRed,
    onError = Color(0xFF2B0705),
    outline = Color(0xFF3A5368),
    outlineVariant = Color(0xFF22394B)
)

/**
 * Inter: una grotesca plana, neutra i elegant, amb suport complet per als
 * accents i la ela geminada del català. Substitueix la serif del disseny
 * inicial perquè tota l'app comparteixi una sola veu tipogràfica.
 */
private val EgeuFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val EgeuTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = Typography().displayMedium.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = Typography().displaySmall.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.4).sp
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = EgeuFont,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)

private val EgeuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun CronosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EgeuScheme,
        typography = EgeuTypography,
        shapes = EgeuShapes,
        content = content
    )
}
