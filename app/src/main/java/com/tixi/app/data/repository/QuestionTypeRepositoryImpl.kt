package com.tixi.app.data.repository

import com.tixi.app.data.local.Mappers
import com.tixi.app.data.local.dao.QuestionTypeDao
import com.tixi.app.domain.model.QuestionTypeEntry
import com.tixi.app.domain.repository.QuestionTypeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionTypeRepositoryImpl @Inject constructor(
    private val dao: QuestionTypeDao,
    private val mappers: Mappers,
) : QuestionTypeRepository {

    /** 题数由 DAO 的 COUNT 子查询补齐，未匹配题型为 0 */
    override fun observeTypes(): Flow<List<QuestionTypeEntry>> =
        dao.observeAllWithCount().map { rows -> rows.map(mappers::questionTypeToDomain) }

    override suspend fun types(): List<QuestionTypeEntry> = withContext(Dispatchers.IO) {
        dao.allWithCount().map(mappers::questionTypeToDomain)
    }

    /**
     * 中文名无法转拼音 slug，自定义题型统一用 `custom_N` 序号键（N 取当前自定义数 +1，
     * 并逐个探测保证唯一）。重名（含内置题型名）直接返回 null。
     */
    override suspend fun addType(name: String): String? = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext null
        if (dao.findByName(trimmed) != null) return@withContext null

        var index = dao.countCustom() + 1
        var key = "custom_$index"
        while (dao.findByKey(key) != null) {
            index += 1
            key = "custom_$index"
        }
        val sortOrder = (dao.maxSortOrder() ?: -1) + 1
        dao.insert(
            mappers.questionTypeToEntity(
                key = key,
                name = trimmed,
                builtIn = false,
                sortOrder = sortOrder,
            ),
        )
        key
    }

    override suspend fun renameType(key: String, name: String) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) dao.updateName(key, trimmed)
    }

    /** 内置三种不可删，静默忽略；删除后已有题目保留，展示时回落为通用题型 */
    override suspend fun deleteType(key: String) = withContext(Dispatchers.IO) {
        val entity = dao.findByKey(key) ?: return@withContext
        if (entity.builtIn) return@withContext
        dao.deleteByKey(key)
    }
}
