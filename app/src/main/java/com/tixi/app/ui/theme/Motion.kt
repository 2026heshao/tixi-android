package com.tixi.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 动效时长，对齐设计规范 §7 */
object TixiMotion {
    const val Instant = 100
    const val Fast = 150
    const val Normal = 250
    const val Slow = 350
}

@Composable
fun tweenFast() = tween<Float>(durationMillis = TixiMotion.Fast)
@Composable
fun tweenNormal() = tween<Float>(durationMillis = TixiMotion.Normal)

/**
 * 系统关闭动画（开发者选项 / 无障碍）时降级为即时切换。
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}
