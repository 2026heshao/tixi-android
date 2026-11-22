package com.tixi.app.data.repository

import com.tixi.app.data.local.Mappers
import com.tixi.app.data.local.dao.CategoryDao
import com.tixi.app.data.local.entity.CategoryEntity
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
    private val mappers: Mappers,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeAllWithCount().map { rows -> rows.map(mappers::categoryToDomain) }

    override fun observeCategory(id: Long): Flow<Category?> =
        dao.observeByIdWithCount(id).map { row -> row?.let(mappers::categoryToDomain) }

    override suspend fun categories(): List<Category> = withContext(Dispatchers.IO) {
        dao.allWithCount().map(mappers::categoryToDomain)
    }

    override suspend fun create(name: String): Long = withContext(Dispatchers.IO) {
        val nextOrder = (dao.maxSortOrder() ?: -1) + 1
        dao.insert(CategoryEntity(name = name.trim(), sortOrder = nextOrder))
    }

    override suspend fun rename(id: Long, name: String) = withContext(Dispatchers.IO) {
        dao.rename(id, name.trim())
    }

    /** DAO 内事务：先把题目置为未分类，再删分类 */
    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteAndDetach(id)
    }
}
