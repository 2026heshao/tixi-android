package com.tixi.app

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 冷启动回归：真机曾出现「白屏后立即闪退」，本用例在 JVM 上真实走一遍
 * Application.onCreate → MainActivity.onCreate → setContent → 首帧组合，
 * 任何启动期异常都会在这里以真实堆栈暴露。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppStartupTest {

    @Test
    fun `冷启动 MainActivity 不崩溃`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        shadowOf(Looper.getMainLooper()).idle()
        check(controller.get() != null) { "Activity 未能创建" }
    }
}
