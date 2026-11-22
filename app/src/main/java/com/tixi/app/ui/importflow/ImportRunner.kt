package com.tixi.app.ui.importflow

import com.tixi.app.data.files.ImportFileStore
import com.tixi.app.data.remote.MinerUClient
import com.tixi.app.domain.model.DraftQuestion
import com.tixi.app.domain.model.ImportError
import com.tixi.app.domain.model.ImportRecord
import com.tixi.app.domain.model.ImportStatus
import com.tixi.app.domain.model.ParseOutcome
import com.tixi.app.domain.model.ParseParams
import com.tixi.app.domain.model.ParseProgress
import com.tixi.app.domain.model.QualityFlag
import com.tixi.app.domain.repository.ImportRepository
import com.tixi.app.domain.repository.SettingsRepository
import com.tixi.app.domain.usecase.DetectGarbage
import com.tixi.app.domain.usecase.MinerUMarkdownParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportRunState {
    data object Idle : ImportRunState
    data class Running(
        val fileName: String,
        val sizeLabel: String,
        val progress: ParseProgress,
    ) : ImportRunState

    data class DraftReady(
        val draftId: Long,
        val fileName: String,
        val questionCount: Int,
        val needsReviewCount: Int,
        val hasGarbage: Boolean,
    ) : ImportRunState

    data class Failed(
        val fileName: String,
        val error: ImportError,
    ) : ImportRunState
}

/** 进度百分比：四阶段共用一条进度条（设计 §6.5） */
fun ParseProgress.overallPercent(): Int = when (this) {
    is ParseProgress.Preparing -> 2
    is ParseProgress.Uploading -> 8
    is ParseProgress.Uploaded -> 20
    is ParseProgress.Queued -> 30
    is ParseProgress.Parsing -> 30 + (percent * 60 / 100)
    is ParseProgress.Downloading -> 92
    is ParseProgress.Done -> 100
}

/**
 * 导入任务的唯一执行者：放在 Singleton 而非 ViewModel 里，
 * 这样离开识别任务页（后台运行）任务不中断。
 */
@Singleton
class ImportRunner @Inject constructor(
    private val client: MinerUClient,
    private val settingsRepository: SettingsRepository,
    private val importRepository: ImportRepository,
    private val fileStore: ImportFileStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<ImportRunState>(ImportRunState.Idle)
    val state: StateFlow<ImportRunState> = _state.asStateFlow()

    private var job: Job? = null
    private var lastFile: File? = null
    private var lastSizeLabel: String = ""

    fun start(file: File, sizeLabel: String) {
        job?.cancel()
        lastFile = file
        lastSizeLabel = sizeLabel
        _state.value = ImportRunState.Running(file.name, sizeLabel, ParseProgress.Preparing)

        job = scope.launch {
            val token = settingsRepository.getToken()
            if (token.isNullOrBlank()) {
                fail(file.name, ImportError.TokenMissing)
                return@launch
            }
            val params: ParseParams = settingsRepository.observeParseParams().first()

            val outcome = client.parse(token, file, params) { progress ->
                val current = _state.value
                if (current is ImportRunState.Running) {
                    _state.value = current.copy(progress = progress)
                }
            }

            when (outcome) {
                is ParseOutcome.Failure -> fail(file.name, outcome.error)
                is ParseOutcome.Success -> {
                    val parsed = MinerUMarkdownParser.parse(outcome.markdown)
                    if (parsed.isEmpty()) {
                        fail(file.name, ImportError.Unknown("解析结果中没有识别到题目，可换一份更清晰的原文重试"))
                        record(file, file.name, 0, ImportStatus.FAILED, "未解析出题目")
                        return@launch
                    }
                    val drafts = parsed.map { item ->
                        val flags = DetectGarbage.evaluate(
                            stem = item.stem,
                            options = item.options,
                            answer = item.answer,
                            richBlocks = item.richBlocks,
                            typeKey = item.typeKey,
                        )
                        DraftQuestion(
                            stem = item.stem,
                            options = item.options,
                            answer = item.answer,
                            explanation = item.explanation,
                            typeKey = item.typeKey,
                            richBlocks = item.richBlocks,
                            qualityFlags = flags,
                            needsReview = DetectGarbage.requiresManualReview(flags),
                        )
                    }
                    val draftId = importRepository.createDraft(
                        sourceFileName = outcome.fileName,
                        sourceFilePath = file.absolutePath,
                        rawResultPath = outcome.rawZipPath,
                        questions = drafts,
                    )
                    val needsReview = drafts.count { it.needsReview }
                    val hasGarbage = drafts.any { d -> d.qualityFlags.contains(QualityFlag.SUSPECT_GARBLED) }
                    val status = when {
                        needsReview == 0 -> ImportStatus.SUCCESS
                        needsReview < drafts.size -> ImportStatus.PARTIAL
                        else -> ImportStatus.PARTIAL
                    }
                    record(
                        file,
                        outcome.fileName,
                        drafts.size,
                        status,
                        if (hasGarbage) "部分题目疑似乱码，需人工核对" else null,
                    )
                    _state.value = ImportRunState.DraftReady(
                        draftId = draftId,
                        fileName = outcome.fileName,
                        questionCount = drafts.size,
                        needsReviewCount = needsReview,
                        hasGarbage = hasGarbage,
                    )
                }
            }
        }
    }

    fun retry() {
        val file = lastFile ?: return
        if (!file.exists()) {
            _state.value = ImportRunState.Failed(file.name, ImportError.BadFile("原始文件已不存在，请重新选择"))
            return
        }
        start(file, lastSizeLabel)
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = ImportRunState.Idle
    }

    fun reset() {
        if (_state.value is ImportRunState.Running) return
        _state.value = ImportRunState.Idle
    }

    fun consumeDraftReady() {
        if (_state.value is ImportRunState.DraftReady) _state.value = ImportRunState.Idle
    }

    private fun fail(fileName: String, error: ImportError) {
        _state.value = ImportRunState.Failed(fileName, error)
        val file = lastFile
        if (file != null) {
            scope.launch { record(file, fileName, 0, ImportStatus.FAILED, error.title) }
        }
    }

    private suspend fun record(
        file: File,
        fileName: String,
        count: Int,
        status: ImportStatus,
        errorMessage: String?,
    ) {
        importRepository.addRecord(
            ImportRecord(
                sourceFileName = fileName,
                sourceType = file.extension.lowercase().ifBlank { "file" },
                importedCount = count,
                status = status,
                errorMessage = errorMessage,
            ),
        )
    }
}
