package com.tixi.app.domain.repository

import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.DraftQuestion
import com.tixi.app.domain.model.ImportDraft
import com.tixi.app.domain.model.ImportRecord
import com.tixi.app.domain.model.ParseParams
import com.tixi.app.domain.model.ParsedQuestion
import com.tixi.app.domain.model.PracticeProgress
import com.tixi.app.domain.model.Question
import com.tixi.app.domain.model.QuestionTypeEntry
import com.tixi.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * 数据层契约。实现类位于 data/repository，UI 与 ViewModel 只依赖这些接口。
 * 约定：categoryId = null 表示「未分类」；Calendar 无关，时间戳统一用 System.currentTimeMillis()。
 */
interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeCategory(id: Long): Flow<Category?>
    suspend fun categories(): List<Category>
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    /** 事务内把该分类题目置为未分类，再删除分类（外键 SET_NULL 双保险） */
    suspend fun delete(id: Long)
}

interface QuestionRepository {
    fun observeAll(): Flow<List<Question>>
    /** categoryId = null 即未分类 */
    fun observeByCategory(categoryId: Long?): Flow<List<Question>>
    fun observeQuestion(id: Long): Flow<Question?>
    suspend fun getQuestion(id: Long): Question?
    suspend fun questionsOfCategory(categoryId: Long?): List<Question>
    suspend fun upsert(question: Question): Long
    suspend fun delete(id: Long)
    suspend fun countAll(): Int
    /** key 为 null 表示未分类 */
    suspend fun countByCategory(): Map<Long?, Int>
    suspend fun countByType(): Map<String, Int>
    /** 题干 LIKE + 标签包含匹配；query 为空时只按分类/标签筛选 */
    suspend fun search(query: String, categoryId: Long? = null, tag: String? = null): List<Question>
    /** 追加到分类末尾：max(sortOrder)+1 */
    suspend fun nextSortOrder(categoryId: Long?): Int
    /** 分类内重排，按传入顺序重写 sortOrder */
    suspend fun reorder(categoryId: Long?, orderedIds: List<Long>)
}

interface QuestionTypeRepository {
    fun observeTypes(): Flow<List<QuestionTypeEntry>>
    suspend fun types(): List<QuestionTypeEntry>
    /** 新增自定义题型，返回生成的 key；名称重复时返回 null */
    suspend fun addType(name: String): String?
    suspend fun renameType(key: String, name: String)
    /** 内置三种不可删；删除后已有题目保留（typeKey 仍存在但回落为通用展示） */
    suspend fun deleteType(key: String)
}

interface ImportRepository {
    fun observeRecords(): Flow<List<ImportRecord>>
    suspend fun addRecord(record: ImportRecord): Long
    suspend fun deleteRecord(id: Long)

    /** 落库草稿批次 + 逐题草稿（qualities 由调用方先算好）；返回 draftId */
    suspend fun createDraft(
        sourceFileName: String,
        sourceFilePath: String,
        rawResultPath: String?,
        questions: List<DraftQuestion>,
    ): Long

    fun observeDraft(draftId: Long): Flow<ImportDraft?>
    suspend fun getDraft(draftId: Long): ImportDraft?
    suspend fun updateDraftQuestion(question: DraftQuestion)

    /**
     * 入库：onlyConfirmedIds = null 表示全部确认并入库；
     * 否则只入库指定 id 的草稿题。返回入库题目数。
     * 含 needsReview 的草稿在批量模式下的拦截由 ViewModel 负责（UI 二次确认）。
     */
    suspend fun confirmDrafts(
        draftId: Long,
        onlyConfirmedIds: Set<Long>?,
        categoryId: Long?,
    ): Int

    suspend fun markDraftStatus(draftId: Long, status: String)
    suspend fun deleteDraft(draftId: Long)
}

interface SettingsRepository {
    fun observeTokenConfigured(): Flow<Boolean>
    /** 仅在需要发起请求时读取；禁止写日志 */
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()

    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    fun observeParseParams(): Flow<ParseParams>
    suspend fun setParseParams(params: ParseParams)

    fun observeOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete(done: Boolean)
}

interface PracticeRepository {
    fun observeProgress(categoryId: Long): Flow<PracticeProgress?>
    suspend fun getProgress(categoryId: Long): PracticeProgress?
    suspend fun saveProgress(categoryId: Long, lastQuestionId: Long)
}
