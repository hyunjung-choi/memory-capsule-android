package com.daljoalab.memorycapsule.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = BubbleGreen,
    onPrimaryContainer = BubbleGreenText,
    secondaryContainer = SecondaryButton,
    onSecondaryContainer = InkMuted,
    surfaceVariant = CaptionGray,
    onSurfaceVariant = InkSoft,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    outline = SecondaryButtonBorder,
    outlineVariant = Divider,
)

@Composable
fun MemoryCapsuleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
