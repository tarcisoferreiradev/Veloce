package com.veloce.presentation.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun VeloceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFFB8FB07), onPrimary = Color(0xFF0F1401),
        background = Color(0xFF101611), surface = Color(0xFF1C261F),
        onBackground = Color(0xFFF2F7ED), onSurface = Color(0xFFF2F7ED),
    ), content = content)
}
