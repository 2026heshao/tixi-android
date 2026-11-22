package com.tixi.app.domain.model

/** 解析四阶段进度，供识别任务页的 Stepper 驱动（设计 §10 #18） */
sealed interface ParseProgress {
    data object Preparing : ParseProgress
    data object Uploading : ParseProgress
    data class Uploaded(val percent: Int) : ParseProgress
    data object Queued : ParseProgress
    data class Parsing(val extractedPages: Int, val totalPages: Int) : ParseProgress {
        val percent: Int get() = if (totalPages <= 0) 0 else (extractedPages * 100 / totalPages).coerceIn(0, 100)
    }
    data object Downloading : ParseProgress
    data object Done : ParseProgress
}

/** 解析结果：成功给出原始 zip 落盘路径 + markdown 正文；失败给出可展示的错误 */
sealed interface ParseOutcome {
    data class Success(
        val markdown: String,
        val rawZipPath: String?,
        val fileName: String,
    ) : ParseOutcome

    data class Failure(val error: ImportError) : ParseOutcome
}

/**
 * 解析错误分档（对齐框架 §5.2 错误码映射）：
 * 每档都必须给出「怎么修」，对应设计规范 §8「可理解的错误文案」。
 */
sealed interface ImportError {
    val title: String
    val hint: String
    val actionLabel: String?

    data object TokenMissing : ImportError {
        override val title = "尚未配置 MinerU Token"
        override val hint = "请先在设置中填入自己的 MinerU Token，再重新导入。"
        override val actionLabel = "去设置 Token"
    }

    data object TokenInvalid : ImportError {
        override val title = "Token 无效（A0202）"
        override val hint = "请检查 Token 是否完整复制，只保留 Bearer 后的部分。"
        override val actionLabel = "去更换 Token"
    }

    data object TokenExpired : ImportError {
        override val title = "Token 已过期（A0211）"
        override val hint = "原始文件已保留，更新 Token 后可直接重试。"
        override val actionLabel = "去更换 Token"
    }

    data object RateLimited : ImportError {
        override val title = "请求过于频繁（429）"
        override val hint = "稍等片刻再重试，或调大设置中的轮询间隔。"
        override val actionLabel = "重试"
    }

    data class BadFile(val reason: String) : ImportError {
        override val title = "文件不符合解析要求"
        override var hint: String = reason
            private set
        override val actionLabel = "重新选择文件"
    }

    data class Server(val message: String?) : ImportError {
        override val title = "识别服务暂时不可用"
        override var hint: String = message?.takeIf { it.isNotBlank() } ?: "服务端返回异常，原始文件已保留，稍后重试。"
            private set
        override val actionLabel = "重试"
    }

    data class Timeout(val minutes: Int) : ImportError {
        override val title = "识别超时"
        override var hint: String = "已等待 $minutes 分钟仍未完成。原始文件已保留，可重试或拆分文件后再试。"
            private set
        override val actionLabel = "重试"
    }

    data object Offline : ImportError {
        override val title = "网络不可用"
        override val hint = "MinerU 解析必须联网，请检查网络后重试。"
        override val actionLabel = "重试"
    }

    data class Unknown(val message: String?) : ImportError {
        override val title = "识别失败"
        override var hint: String = message?.takeIf { it.isNotBlank() } ?: "未知错误，原始文件已保留，可重试。"
            private set
        override val actionLabel = "重试"
    }
}
