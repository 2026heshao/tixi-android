package com.tixi.app.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

/**
 * 崩溃取证页。
 *
 * 刻意不使用 [com.tixi.app.ui.theme.TixiTheme] 与自定义字体：即便崩溃原因是字体或主题，
 * 本页也必须能正常渲染，否则用户拿不到任何信息。
 */
private val Ink = Color(0xFF1B1D21)
private val Paper = Color(0xFFF7F4EC)
private val Muted = Color(0xFF6B6F76)
private val Accent = Color(0xFF8C3A2E)
private val Rule = Color(0xFFD8D2C4)
private val CodeBg = Color(0xFF2A2C31)

@Composable
fun CrashScreen(
    report: String,
    onClearAndRetry: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            "上次启动崩溃了",
            modifier = Modifier.padding(top = 20.dp),
            color = Accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "下面这段信息是崩溃现场，复制或分享给我即可定位。",
            modifier = Modifier.padding(top = 6.dp),
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )

        Box(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CodeBg)
                .border(1.dp, Rule, RoundedCornerShape(8.dp)),
        ) {
            Text(
                text = report,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                color = Color(0xFFE8E4D8),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 15.sp,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CrashAction("复制全部", Modifier.weight(1f)) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("题隙崩溃日志", report))
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            }
            CrashAction("分享文件", Modifier.weight(1f)) {
                shareReport(context, report)
            }
            CrashAction("清除并重试", Modifier.weight(1f), filled = true) {
                CrashRecorder.clear(context)
                onClearAndRetry()
            }
        }
    }
}

@Composable
private fun CrashAction(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) Ink else Color.Transparent)
            .border(1.dp, if (filled) Ink else Rule, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (filled) Paper else Ink,
            fontSize = 13.sp,
        )
    }
}

private fun shareReport(context: Context, report: String) {
    runCatching {
        val file = File(File(context.filesDir, "diagnostics"), "crash-report.txt")
        file.parentFile?.mkdirs()
        file.writeText(report)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "题隙 · 崩溃日志")
            putExtra(Intent.EXTRA_TEXT, report.take(2000))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享崩溃日志").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.onFailure {
        Toast.makeText(context, "分享失败：${it.message}", Toast.LENGTH_LONG).show()
    }
}
