package com.tixi.app.data.repository

import com.tixi.app.data.local.Mappers
import com.tixi.app.data.local.dao.QuestionDao
import com.tixi.app.domain.model.Question
import com.tixi.app.domain.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepositoryImpl @Inject constructor(
    private val dao: QuestionDao,
    private val mappers: Mappers,
) : QuestionRepository {

    override fun observeAll(): Flow<List<Question>> =
        dao.observeAll().map { list -> list.map(mappers::questionToDomain) }

    override fun observeByCategory(categoryId: Long?): Flow<List<Question>> =
        dao.observeByCategory(categoryId).map { list -> list.map(mappers::questionToDomain) }

    override fun observeQuestion(id: Long): Flow<Question?> =
        dao.observeById(id).map { entity -> entity?.let(mappers::questionToDomain) }

    override suspend fun getQuestion(id: Long): Question? = withContext(Dispatchers.IO) {
        dao.findById(id)?.let(mappers::questionToDomain)
    }

    override suspend fun questionsOfCategory(categoryId: Long?): List<Question> =
        withContext(Dispatchers.IO) {
            dao.byCategory(categoryId).map(mappers::questionToDomain)
        }

    /** id = 0 走插入，否则按主键更新；updatedAt 统一刷新 */
    override suspend fun upsert(question: Question): Long = withContext(Dispatchers.IO) {
        val entity = mappers.questionToEntity(question, System.currentTimeMillis())
        if (question.id == 0L) dao.insert(entity) else {
            dao.upsert(entity)
            question.id
        }
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    override suspend fun countAll(): Int = withContext(Dispatchers.IO) { dao.count() }

    override suspend fun countByCategory(): Map<Long?, Int> = withContext(Dispatchers.IO) {
        dao.countByCategory().associate { it.categoryId to it.cnt }
    }

    override suspend fun countByType(): Map<String, Int> = withContext(Dispatchers.IO) {
        dao.countByType().associate { it.questionType to it.cnt }
    }

    /** categoryId = null 表示全库搜索（不按分类过滤）；tag 为子串匹配 */
    override suspend fun search(query: String, categoryId: Long?, tag: String?): List<Question> =
        withContext(Dispatchers.IO) {
            val keyword = query.trim()
            val tagKeyword = tag?.trim()?.takeIf { it.isNotEmpty() }
            dao.search(keyword, categoryId, tagKeyword).map(mappers::questionToDomain)
        }

    override suspend fun nextSortOrder(categoryId: Long?): Int = withContext(Dispatchers.IO) {
        (dao.maxSortOrder(categoryId) ?: -1) + 1
    }

    /** DAO 内事务逐条重写 sortOrder，入参顺序即新顺序 */
    override suspend fun reorder(categoryId: Long?, orderedIds: List<Long>) =
        withContext(Dispatchers.IO) {
            dao.reorder(orderedIds)
        }
}
