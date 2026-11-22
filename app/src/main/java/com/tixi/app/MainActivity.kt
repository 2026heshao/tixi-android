package com.tixi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tixi.app.diagnostics.CrashRecorder
import com.tixi.app.diagnostics.CrashScreen
import com.tixi.app.ui.navigation.TixiAppRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        CrashRecorder.stage(this, "MainActivity:enter")
        super.onCreate(savedInstanceState)
        CrashRecorder.stage(this, "MainActivity:super-ok")
        enableEdgeToEdge()
        CrashRecorder.stage(this, "MainActivity:edge2edge-ok")

        // 上一次启动崩过：先给取证页，用户可复制/分享堆栈后再重试（仅调试包呈现）
        val lastCrash = if (BuildConfig.DEBUG) CrashRecorder.readLastCrash(this) else null
        if (lastCrash != null) {
            CrashRecorder.stage(this, "MainActivity:show-crash-screen")
            setContent {
                CrashScreen(report = lastCrash, onClearAndRetry = { recreate() })
            }
        } else {
            setContent {
                TixiAppRoot()
            }
        }
    }
}
