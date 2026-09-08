package com.filldemaia.cronos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme()

private val DarkColors = darkColorScheme()

@Composable
fun CronosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val timePalette = currentTimePalette()

    CompositionLocalProvider(LocalTimePalette provides timePalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CronosTypography,
            content = content,
        )
    }
}
