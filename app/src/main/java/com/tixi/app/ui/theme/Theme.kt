package com.tixi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun TixiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) DarkTixiColors else LightTixiColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.textPrimary,
            secondary = c.secondary,
            onSecondary = c.onSecondary,
            background = c.canvas,
            onBackground = c.textPrimary,
            surface = c.surface,
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.textSecondary,
            outline = c.outline,
            error = c.error,
            onError = c.onPrimary,
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.textPrimary,
            secondary = c.secondary,
            onSecondary = c.onSecondary,
            background = c.canvas,
            onBackground = c.textPrimary,
            surface = c.surface,
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.textSecondary,
            outline = c.outline,
            error = c.error,
            onError = Color(0xFFFFFCF5),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalTixiColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = TixiTypography,
            shapes = TixiShapes,
            content = content,
        )
    }
}

/** 语义色快捷入口：TixiTheme.colors.xxx */
object TixiTheme {
    val colors: TixiColors
        @Composable get() = LocalTixiColors.current
}
