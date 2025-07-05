package com.example.kiendatn2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

private val customColorScheme = CustomColorScheme(
    primaryBackground = primaryBackground,
    secondaryBackground = secondaryBackground,
    isSelectedBackground = isSelectedBackground,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textIsLiked = textIsLiked,
    navBarsBackground = navBarsBackground,
    fabBackground = fabBackground,
    iconColor = iconColor,
    sharedColor = sharedColor
)

val LocalCustomColors = compositionLocalOf { customColorScheme }

@Composable
fun Kiendatn2Theme(
    content: @Composable () -> Unit
) {
    val materialColorScheme = darkColorScheme(
        primary = LocalCustomColors.current.textPrimary,
    )

    CompositionLocalProvider(
        LocalCustomColors provides customColorScheme
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}

data class CustomColorScheme(
    val primaryBackground: Color,
    val secondaryBackground: Color,
    val isSelectedBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textIsLiked: Color,
    val navBarsBackground: Color,
    val fabBackground: Color,
    val iconColor: Color,
    val sharedColor: Color,
)