package com.tixi.app.data.local

import com.tixi.app.data.local.dao.CategoryRow
import com.tixi.app.data.local.dao.QuestionTypeRow
import com.tixi.app.data.local.entity.DraftQuestionEntity
import com.tixi.app.data.local.entity.ImportDraftEntity
import com.tixi.app.data.local.entity.ImportRecordEntity
import com.tixi.app.data.local.entity.PracticeProgressEntity
import com.tixi.app.data.local.entity.QuestionEntity
import com.tixi.app.data.local.entity.QuestionTypeEntity
import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.DraftQuestion
import com.tixi.app.domain.model.DraftStatus
import com.tixi.app.domain.model.ImportDraft
import com.tixi.app.domain.model.ImportRecord
import com.tixi.app.domain.model.ImportStatus
import com.tixi.app.domain.model.PracticeProgress
import com.tixi.app.domain.model.QualityFlag
import com.tixi.app.domain.model.Question
import com.tixi.app.domain.model.QuestionTypeEntry
import com.tixi.app.domain.model.RichBlock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * entity ⇄ domain 映射。JSON 列的解析全部 try-catch 容错：
 * 脏数据退化为空集合 / [AnswerData.Empty]，绝不让界面因为一条坏记录而崩。
 */
@Singleton
class Mappers @Inject constructor(
    private val json: Json,
) {

    // ---- 分类 ----

    fun categoryToDomain(row: CategoryRow): Category = Category(
        id = row.id,
        name = row.name,
        sortOrder = row.sortOrder,
        questionCount = row.questionCount,
    )

    // ---- 题型 ----

    fun questionTypeToDomain(row: QuestionTypeRow): QuestionTypeEntry = QuestionTypeEntry(
        id = row.id,
        key = row.typeKey,
        name = row.name,
        builtIn = row.builtIn,
        sortOrder = row.sortOrder,
        questionCount = row.questionCount,
    )

    fun questionTypeToEntity(
        key: String,
        name: String,
        builtIn: Boolean,
        sortOrder: Int,
    ): QuestionTypeEntity = QuestionTypeEntity(
        key = key,
        name = name,
        builtIn = builtIn,
        sortOrder = sortOrder,
    )

    // ---- 题目 ----

    fun questionToDomain(entity: QuestionEntity): Question = Question(
        id = entity.id,
        categoryId = entity.categoryId,
        stem = entity.stem,
        options = decodeStringList(entity.optionsJson),
        answer = decodeAnswer(entity.answerJson),
        explanation = entity.explanation,
        typeKey = entity.questionType,
        richBlocks = decodeRichBlocks(entity.richBlocksJson),
        sortOrder = entity.sortOrder,
        sourceFile = entity.sourceFile,
        sourceImagePath = entity.sourceImagePath,
        tags = decodeStringList(entity.tagsJson),
        remark = entity.remark,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    /** 写入数据库用；createdAt 缺失时补当前时间，updatedAt 一律刷新 */
    fun questionToEntity(question: Question, now: Long): QuestionEntity = QuestionEntity(
        id = question.id,
        categoryId = question.categoryId,
        stem = question.stem,
        optionsJson = encodeStringList(question.options),
        answerJson = encodeAnswer(question.answer),
        explanation = question.explanation,
        questionType = question.typeKey,
        richBlocksJson = encodeRichBlocks(question.richBlocks),
        sortOrder = question.sortOrder,
        sourceFile = question.sourceFile,
        sourceImagePath = question.sourceImagePath,
        tagsJson = encodeStringList(question.tags),
        remark = question.remark,
        createdAt = if (question.createdAt > 0L) question.createdAt else now,
        updatedAt = now,
    )

    // ---- 导入 ----

    fun importRecordToDomain(entity: ImportRecordEntity): ImportRecord = ImportRecord(
        id = entity.id,
        sourceFileName = entity.sourceFileName,
        sourceType = entity.sourceType,
        importedCount = entity.importedCount,
        status = parseImportStatus(entity.status),
        errorMessage = entity.errorMessage,
        createdAt = entity.createdAt,
    )

    fun importRecordToEntity(record: ImportRecord): ImportRecordEntity = ImportRecordEntity(
        id = record.id,
        sourceFileName = record.sourceFileName,
        sourceType = record.sourceType,
        importedCount = record.importedCount,
        status = record.status.name.lowercase(),
        errorMessage = record.errorMessage,
        createdAt = record.createdAt,
    )

    fun importDraftToDomain(
        draft: ImportDraftEntity,
        questions: List<DraftQuestionEntity>,
    ): ImportDraft = ImportDraft(
        id = draft.id,
        sourceFileName = draft.sourceFileName,
        sourceFilePath = draft.sourceFilePath,
        rawResultPath = draft.rawResultPath,
        status = parseDraftStatus(draft.status),
        createdAt = draft.createdAt,
        questions = questions.map(::draftQuestionToDomain),
    )

    fun draftQuestionToDomain(entity: DraftQuestionEntity): DraftQuestion = DraftQuestion(
        id = entity.id,
        draftId = entity.draftId,
        stem = entity.stem,
        options = decodeStringList(entity.optionsJson),
        answer = decodeAnswer(entity.answerJson),
        explanation = entity.explanation,
        typeKey = entity.questionType,
        richBlocks = decodeRichBlocks(entity.richBlocksJson),
        suggestedCategoryId = entity.suggestedCategoryId,
        qualityFlags = decodeQualityFlags(entity.qualityFlagsJson),
        needsReview = entity.needsReview,
        confirmed = entity.confirmed,
    )

    /** 草稿题就地保存（编辑页回写） */
    fun draftQuestionToEntity(question: DraftQuestion): DraftQuestionEntity = DraftQuestionEntity(
        id = question.id,
        draftId = question.draftId,
        stem = question.stem,
        optionsJson = encodeStringList(question.options),
        answerJson = encodeAnswerOrNull(question.answer),
        explanation = question.explanation,
        questionType = question.typeKey,
        richBlocksJson = encodeRichBlocks(question.richBlocks),
        suggestedCategoryId = question.suggestedCategoryId,
        qualityFlagsJson = encodeQualityFlags(question.qualityFlags),
        needsReview = question.needsReview,
        confirmed = question.confirmed,
    )

    /** 确认入库：草稿题 → 正式题目（分类与排序由调用方算好） */
    fun draftQuestionToQuestionEntity(
        draft: DraftQuestionEntity,
        categoryId: Long?,
        sortOrder: Int,
        now: Long,
    ): QuestionEntity = QuestionEntity(
        categoryId = categoryId,
        stem = draft.stem,
        optionsJson = draft.optionsJson,
        answerJson = draft.answerJson ?: encodeAnswer(AnswerData.Empty),
        explanation = draft.explanation,
        questionType = draft.questionType,
        richBlocksJson = draft.richBlocksJson,
        sortOrder = sortOrder,
        tagsJson = null,
        createdAt = now,
        updatedAt = now,
    )

    // ---- 练习进度 ----

    fun practiceProgressToDomain(entity: PracticeProgressEntity): PracticeProgress = PracticeProgress(
        categoryId = entity.categoryId,
        lastQuestionId = entity.lastQuestionId,
        updatedAt = entity.updatedAt,
    )

    // ---- JSON 编解码（容错） ----

    private fun encodeStringList(list: List<String>?): String? {
        if (list.isNullOrEmpty()) return null
        return runCatching { json.encodeToString(list) }.getOrNull()
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    private fun encodeAnswer(answer: AnswerData): String =
        runCatching { json.encodeToString(answer) }.getOrDefault("""{"correctKeys":[]}""")

    private fun encodeAnswerOrNull(answer: AnswerData): String? =
        if (answer.isEmpty()) null else runCatching { json.encodeToString(answer) }.getOrNull()

    private fun decodeAnswer(raw: String?): AnswerData {
        if (raw.isNullOrBlank()) return AnswerData.Empty
        return runCatching { json.decodeFromString<AnswerData>(raw) }.getOrDefault(AnswerData.Empty)
    }

    private fun encodeRichBlocks(blocks: List<RichBlock>?): String? {
        if (blocks.isNullOrEmpty()) return null
        return runCatching { json.encodeToString(blocks) }.getOrNull()
    }

    private fun decodeRichBlocks(raw: String?): List<RichBlock> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<RichBlock>>(raw) }.getOrDefault(emptyList())
    }

    private fun encodeQualityFlags(flags: List<QualityFlag>?): String? {
        if (flags.isNullOrEmpty()) return null
        return encodeStringList(flags.map { it.name })
    }

    private fun decodeQualityFlags(raw: String?): List<QualityFlag> =
        decodeStringList(raw).mapNotNull { name ->
            QualityFlag.entries.firstOrNull { it.name == name }
        }

    private fun parseDraftStatus(raw: String?): DraftStatus =
        DraftStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DraftStatus.PENDING

    private fun parseImportStatus(raw: String?): ImportStatus =
        ImportStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ImportStatus.SUCCESS
}
