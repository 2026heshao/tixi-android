package com.tixi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val TixiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

object TixiRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 999.dp
}

/** 4pt 基准，8pt 节奏 */
object TixiSpacing {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s6 = 24.dp
    val s8 = 32.dp
    val s12 = 48.dp
    val s16 = 64.dp

    /** 页面左右边距 */
    val pageHorizontal = 16.dp
    val minTouchTarget = 48.dp
    val contentMaxWidth = 600.dp
}

object TixiSize {
    val topBar = 56.dp
    val topBarLarge = 112.dp
    val bottomNav = 64.dp
    val buttonLarge = 48.dp
    val buttonMedium = 40.dp
    val buttonSmall = 32.dp
    val field = 56.dp
    val fillField = 52.dp
    val categoryCard = 96.dp
    val importCard = 88.dp
    val questionRow = 72.dp
    val settingRow = 56.dp
    val progress = 4.dp
}
