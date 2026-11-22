package com.tixi.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.tixi.app.R

/**
 * 字体族：标题/题干用衬线（纸本气质），正文用系统无衬线，数字用等宽。
 *
 * 衬线族内置 **Noto Serif SC 子集**（OFL-1.1，见 assets/licenses/OFL-NotoSerifSC.txt）：
 * 覆盖 7896 个码位（GB 常用汉字 + 拉丁 + 全角标点 + 数学符号），每字重 2.36 MB。
 * 只绑定 Normal / Medium 两档，与 [TixiTypography] 用到的字重一致；
 * 子集外的生僻字由系统回退链兜底，不会出现豆腐块。
 * 字体只作用于标题与题干，不全局替换正文。
 */
object TixiFontFamilies {
    val Serif: FontFamily = FontFamily(
        Font(R.font.noto_serif_sc_400, FontWeight.Normal),
        Font(R.font.noto_serif_sc_500, FontWeight.Medium),
    )
    val Sans: FontFamily = FontFamily.SansSerif
    val Mono: FontFamily = FontFamily.Monospace
}

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, serif: Boolean = false) = TextStyle(
    fontFamily = if (serif) TixiFontFamilies.Serif else TixiFontFamilies.Sans,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    lineHeightStyle = tightLineHeight,
)

val TixiTypography = Typography(
    displaySmall = style(28, 36, FontWeight.Medium, serif = true),
    headlineSmall = style(24, 32, FontWeight.Medium, serif = true),
    titleLarge = style(20, 28, FontWeight.Medium, serif = true),
    titleMedium = style(18, 26, FontWeight.Medium, serif = true),
    bodyLarge = style(16, 26, FontWeight.Normal),
    bodyMedium = style(14, 22, FontWeight.Normal),
    bodySmall = style(12, 18, FontWeight.Normal),
    labelSmall = style(11, 16, FontWeight.Medium),
    labelMedium = style(12, 18, FontWeight.Medium),
    labelLarge = style(13, 18, FontWeight.Medium),
)
