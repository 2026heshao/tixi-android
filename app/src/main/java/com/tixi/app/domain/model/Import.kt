package com.tixi.app.domain.model

/** 识别草稿批次（失败/乱码均保留，可重试或手工编辑） */
data class ImportDraft(
    val id: Long,
    val sourceFileName: String,
    val sourceFilePath: String,
    val rawResultPath: String? = null,
    val status: DraftStatus = DraftStatus.PENDING,
    val createdAt: Long = 0,
    val questions: List<DraftQuestion> = emptyList(),
) {
    val confirmedCount: Int get() = questions.count { it.confirmed }
    val needsReviewCount: Int get() = questions.count { it.needsReview }
    val hasGarbage: Boolean get() = questions.any { q -> q.qualityFlags.any { it == QualityFlag.SUSPECT_GARBLED } }
}

enum class DraftStatus { PENDING, CONFIRMED, DISCARDED }

data class DraftQuestion(
    val id: Long = 0,
    val draftId: Long = 0,
    val stem: String,
    val options: List<String> = emptyList(),
    val answer: AnswerData = AnswerData.Empty,
    val explanation: String? = null,
    val typeKey: String = BuiltInQuestionType.CHOICE.key,
    val richBlocks: List<RichBlock> = emptyList(),
    val suggestedCategoryId: Long? = null,
    val qualityFlags: List<QualityFlag> = emptyList(),
    val needsReview: Boolean = false,
    val confirmed: Boolean = false,
) {
    val builtInType: BuiltInQuestionType? get() = BuiltInQuestionType.fromKey(typeKey)
    val hasRichContent: Boolean get() = richBlocks.isNotEmpty()
}

/** 导入记录：只留来源/数量/成败原因，去重与导出后置 */
data class ImportRecord(
    val id: Long = 0,
    val sourceFileName: String,
    val sourceType: String,
    val importedCount: Int = 0,
    val status: ImportStatus = ImportStatus.SUCCESS,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ImportStatus(val label: String) {
    SUCCESS("成功"), PARTIAL("部分成功"), FAILED("失败")
}

/** MinerU 输出版本无关的题目草稿（解析层产物，未入库） */
data class ParsedQuestion(
    val stem: String,
    val options: List<String> = emptyList(),
    val answer: AnswerData = AnswerData.Empty,
    val explanation: String? = null,
    val typeKey: String = BuiltInQuestionType.CHOICE.key,
    val richBlocks: List<RichBlock> = emptyList(),
)
