package com.tixi.app

import android.app.Application
import com.tixi.app.diagnostics.CrashRecorder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TixiApp : Application() {
    override fun onCreate() {
        // 先装崩溃捕获（纯 Android API，不依赖 Hilt），再进 super：即便 Hilt 组件创建失败也能留证据
        CrashRecorder.install(this)
        CrashRecorder.stage(this, "TixiApp.onCreate:enter")
        super.onCreate()
        CrashRecorder.stage(this, "TixiApp.onCreate:hilt-ready")
    }
}
