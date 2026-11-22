package com.tixi.app.ui.importflow

import androidx.lifecycle.ViewModel
import com.tixi.app.domain.model.ParseProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ImportTaskViewModel @Inject constructor(
    private val runner: ImportRunner,
) : ViewModel() {

    val state: StateFlow<ImportRunState> = runner.state

    fun cancel() = runner.cancel()
    fun retry() = runner.retry()
    fun reset() = runner.reset()
    fun consumeDraftReady() = runner.consumeDraftReady()

    /** 0 上传 1 排队 2 识别 3 下载 */
    fun stageIndex(progress: ParseProgress): Int = when (progress) {
        is ParseProgress.Preparing, is ParseProgress.Uploading -> 0
        is ParseProgress.Uploaded, is ParseProgress.Queued -> 1
        is ParseProgress.Parsing -> 2
        is ParseProgress.Downloading, is ParseProgress.Done -> 3
    }

    fun stageText(progress: ParseProgress): String = when (progress) {
        is ParseProgress.Preparing -> "准备上传…"
        is ParseProgress.Uploading -> "正在上传原始文件…"
        is ParseProgress.Uploaded -> "文件已上传，等待排队"
        is ParseProgress.Queued -> "已通过鉴权，排队中…"
        is ParseProgress.Parsing -> "MinerU 正在解析题目… ${progress.extractedPages}/${progress.totalPages} 页"
        is ParseProgress.Downloading -> "正在下载解析结果…"
        is ParseProgress.Done -> "解析完成，正在生成草稿…"
    }
}
