package com.tixi.app.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 启动期崩溃取证器。
 *
 * 真机上闪退时开发者拿不到 logcat，本类做两件事：
 * 1. [stage] 在启动关键节点覆写一行「最后到达阶段」，即使崩溃早于处理器安装也能定位；
 * 2. 安装全局未捕获异常处理器，把堆栈写入 filesDir，下次启动由 [CrashScreen] 呈现。
 *
 * 约束：崩溃路径上不得抛异常、不得依赖 Hilt、不得做网络/数据库操作。
 */
object CrashRecorder {

    private const val DIR = "diagnostics"
    private const val CRASH_FILE = "last_crash.txt"
    private const val STAGE_FILE = "last_stage.txt"

    private val lock = Any()

    @Volatile
    private var installed = false

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    private fun crashFile(context: Context) = File(dir(context), CRASH_FILE)

    private fun stageFile(context: Context) = File(dir(context), STAGE_FILE)

    /** 记录一个启动阶段；只保留最后一次，写入极小，可在主线程调用 */
    fun stage(context: Context, name: String) {
        runCatching {
            val line = "${timestamp()} | ${Build.MANUFACTURER} ${Build.MODEL} | API ${Build.VERSION.SDK_INT} | $name"
            synchronized(lock) {
                FileOutputStream(stageFile(context), false).use { out ->
                    out.write(line.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    /** 安装全局崩溃捕获；必须在 Application.onCreate 里尽早调用 */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { persist(app, thread, error) }
            // 交回系统默认处理器，保证进程被正常终止并留下系统日志
            previous?.uncaughtException(thread, error)
                ?: android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun persist(context: Context, thread: Thread, error: Throwable) {
        val stack = StringWriter().also { sw ->
            PrintWriter(sw).use { pw ->
                pw.println("时间: ${timestamp()}")
                pw.println("设备: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                pw.println("进程: ${context.packageName} / 线程: ${thread.name}")
                pw.println("最后阶段: ${readStage(context) ?: "未记录"}")
                pw.println("App 版本: ${versionName(context)}")
                pw.println()
                error.printStackTrace(pw)
                // 连锁异常（如 ExceptionInInitializerError / InvocationTargetException）关键
                var cause: Throwable? = error.cause
                var depth = 0
                while (cause != null && depth < 5) {
                    pw.println()
                    pw.println("--- cause #${depth + 1} ---")
                    cause.printStackTrace(pw)
                    cause = cause.cause
                    depth++
                }
            }
        }.toString()

        val f = crashFile(context)
        synchronized(lock) {
            FileOutputStream(f, false).use { it.write(stack.toByteArray(Charsets.UTF_8)) }
        }
    }

    fun readLastCrash(context: Context): String? = runCatching {
        crashFile(context).takeIf { it.exists() }?.readText(Charsets.UTF_8)
    }.getOrNull()

    fun readStage(context: Context): String? = runCatching {
        stageFile(context).takeIf { it.exists() }?.readText(Charsets.UTF_8)
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { crashFile(context).delete() }
        runCatching { stageFile(context).delete() }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    private fun versionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }.getOrDefault("?")
}
