package com.tixi.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 语义色板：浅色为「纸面」，深色为「夜读墨面」。两套不是简单反色，而是同一语义的两种明度表达。
 * 值严格对齐《题隙-设计规范系统》§1.2 / §1.3。
 */
@Immutable
data class TixiColors(
    val canvas: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val outline: Color,
    val outlineStrong: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val isDark: Boolean,
) {
    /** 语义色的浅底（用于选中/警告块），保持与 surface 混合后的可读层次 */
    val successContainer: Color get() = if (isDark) Color(0xFF22322A) else Color(0xFFE7F0EA)
    val warningContainer: Color get() = if (isDark) Color(0xFF332B1D) else Color(0xFFF5EEDD)
    val errorContainer: Color get() = if (isDark) Color(0xFF35221F) else Color(0xFFF6E7E5)
    val primaryContainer: Color get() = if (isDark) Color(0xFF23303F) else Color(0xFFE6EBF1)
}

val LightTixiColors = TixiColors(
    canvas = Color(0xFFF6F3EB),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFEDE8DC),
    primary = Color(0xFF1F3A5F),
    onPrimary = Color(0xFFFFFCF5),
    secondary = Color(0xFFA8563C),
    onSecondary = Color(0xFFFFFDF8),
    tertiary = Color(0xFF4A6572),
    textPrimary = Color(0xFF1C2B3A),
    textSecondary = Color(0xFF5C6470),
    textTertiary = Color(0xFF666B74),
    outline = Color(0xFFD6D0C2),
    outlineStrong = Color(0xFFA8563C),
    success = Color(0xFF2E6B4F),
    warning = Color(0xFF855B16),
    error = Color(0xFFA33A32),
    info = Color(0xFF1F3A5F),
    isDark = false,
)

val DarkTixiColors = TixiColors(
    canvas = Color(0xFF14161A),
    surface = Color(0xFF1E2024),
    surfaceVariant = Color(0xFF272A2F),
    primary = Color(0xFF7FA8D4),
    onPrimary = Color(0xFF101820),
    secondary = Color(0xFFD08B6A),
    onSecondary = Color(0xFF1A1410),
    tertiary = Color(0xFF8FA8B8),
    textPrimary = Color(0xFFE8E4DC),
    textSecondary = Color(0xFFA8A9AD),
    textTertiary = Color(0xFF94969C),
    outline = Color(0xFF3A3D44),
    outlineStrong = Color(0xFF666A72),
    success = Color(0xFF7FB893),
    warning = Color(0xFFD4A24C),
    error = Color(0xFFE07B73),
    info = Color(0xFF7FA8D4),
    isDark = true,
)

val LocalTixiColors = staticCompositionLocalOf { LightTixiColors }
