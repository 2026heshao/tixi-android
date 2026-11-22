package com.tixi.app.data.repository

import com.tixi.app.data.local.Mappers
import com.tixi.app.data.local.dao.PracticeDao
import com.tixi.app.data.local.entity.PracticeProgressEntity
import com.tixi.app.domain.model.PracticeProgress
import com.tixi.app.domain.repository.PracticeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeRepositoryImpl @Inject constructor(
    private val dao: PracticeDao,
    private val mappers: Mappers,
) : PracticeRepository {

    override fun observeProgress(categoryId: Long): Flow<PracticeProgress?> =
        dao.observeProgress(categoryId).map { entity -> entity?.let(mappers::practiceProgressToDomain) }

    override suspend fun getProgress(categoryId: Long): PracticeProgress? = withContext(Dispatchers.IO) {
        dao.findProgress(categoryId)?.let(mappers::practiceProgressToDomain)
    }

    /** 只在内存记「上次练到哪」，退出即丢统计；这里仅持久化断点 */
    override suspend fun saveProgress(categoryId: Long, lastQuestionId: Long) =
        withContext(Dispatchers.IO) {
            dao.upsert(
                PracticeProgressEntity(
                    categoryId = categoryId,
                    lastQuestionId = lastQuestionId,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
}
