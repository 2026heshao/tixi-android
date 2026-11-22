package com.tixi.app.data.repository

import androidx.room.withTransaction
import com.tixi.app.data.local.Mappers
import com.tixi.app.data.local.TixiDatabase
import com.tixi.app.data.local.dao.ImportDao
import com.tixi.app.data.local.dao.QuestionDao
import com.tixi.app.data.local.entity.ImportDraftEntity
import com.tixi.app.domain.model.DraftQuestion
import com.tixi.app.domain.model.ImportDraft
import com.tixi.app.domain.model.ImportRecord
import com.tixi.app.domain.repository.ImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepositoryImpl @Inject constructor(
    private val db: TixiDatabase,
    private val dao: ImportDao,
    private val questionDao: QuestionDao,
    private val mappers: Mappers,
) : ImportRepository {

    override fun observeRecords(): Flow<List<ImportRecord>> =
        dao.observeRecords().map { rows -> rows.map(mappers::importRecordToDomain) }

    override suspend fun addRecord(record: ImportRecord): Long = withContext(Dispatchers.IO) {
        dao.insertRecord(mappers.importRecordToEntity(record))
    }

    override suspend fun deleteRecord(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteRecord(id)
    }

    override suspend fun createDraft(
        sourceFileName: String,
        sourceFilePath: String,
        rawResultPath: String?,
        questions: List<DraftQuestion>,
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val draftId = dao.insertDraft(
                ImportDraftEntity(
                    sourceFileName = sourceFileName,
                    sourceFilePath = sourceFilePath,
                    rawResultPath = rawResultPath,
                    status = "pending",
                ),
            )
            if (questions.isNotEmpty()) {
                dao.insertDraftQuestions(
                    questions.map { question ->
                        mappers.draftQuestionToEntity(question).copy(draftId = draftId)
                    },
                )
            }
            draftId
        }
    }

    override fun observeDraft(draftId: Long): Flow<ImportDraft?> =
        dao.observeDraftWithQuestions(draftId).map { result ->
            result?.let { mappers.importDraftToDomain(it.draft, it.questions) }
        }

    override suspend fun getDraft(draftId: Long): ImportDraft? = withContext(Dispatchers.IO) {
        dao.getDraftWithQuestions(draftId)?.let { mappers.importDraftToDomain(it.draft, it.questions) }
    }

    override suspend fun updateDraftQuestion(question: DraftQuestion) = withContext(Dispatchers.IO) {
        dao.updateDraftQuestion(mappers.draftQuestionToEntity(question))
    }

    /**
     * 事务内完成：筛出待入库草稿题 → 逐条映射为正式题目并插入（分类取入参优先，
     * 否则用建议分类；sortOrder 从该分类既有 max+1 依次递增）→ 标记草稿题已确认 →
     * 全部题确认完毕时把批次状态置 confirmed。返回本次真正入库的条数。
     */
    override suspend fun confirmDrafts(
        draftId: Long,
        onlyConfirmedIds: Set<Long>?,
        categoryId: Long?,
    ): Int = withContext(Dispatchers.IO) {
        db.withTransaction {
            val rows = dao.draftQuestions(draftId)
            val selected = if (onlyConfirmedIds == null) rows else rows.filter { it.id in onlyConfirmedIds }
            if (selected.isEmpty()) return@withTransaction 0

            val now = System.currentTimeMillis()
            val nextOrderOf = mutableMapOf<Long?, Int>()
            var imported = 0
            selected.forEach { row ->
                val targetCategoryId = categoryId ?: row.suggestedCategoryId
                val sortOrder = nextOrderOf.getOrPut(targetCategoryId) {
                    questionDao.maxSortOrder(targetCategoryId)?.plus(1) ?: 0
                }
                nextOrderOf[targetCategoryId] = sortOrder + 1

                questionDao.insert(
                    mappers.draftQuestionToQuestionEntity(
                        draft = row,
                        categoryId = targetCategoryId,
                        sortOrder = sortOrder,
                        now = now,
                    ),
                )
                dao.setConfirmed(row.id, true)
                imported += 1
            }

            if (dao.countUnconfirmed(draftId) == 0) {
                dao.updateDraftStatus(draftId, "confirmed")
            }
            imported
        }
    }

    override suspend fun markDraftStatus(draftId: Long, status: String) =
        withContext(Dispatchers.IO) {
            dao.updateDraftStatus(draftId, status)
        }

    override suspend fun deleteDraft(draftId: Long) = withContext(Dispatchers.IO) {
        dao.deleteDraft(draftId)
    }
}
