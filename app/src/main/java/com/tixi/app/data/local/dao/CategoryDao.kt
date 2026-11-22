package com.tixi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tixi.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** 分类 + 题目数（列表页角标），一次查询搞定，避免 N+1 */
data class CategoryRow(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val questionCount: Int,
)

@Dao
abstract class CategoryDao {

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.sortOrder AS sortOrder,
               (SELECT COUNT(*) FROM questions q WHERE q.categoryId = c.id) AS questionCount
        FROM categories c
        ORDER BY c.sortOrder ASC, c.id ASC
        """,
    )
    abstract fun observeAllWithCount(): Flow<List<CategoryRow>>

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.sortOrder AS sortOrder,
               (SELECT COUNT(*) FROM questions q WHERE q.categoryId = c.id) AS questionCount
        FROM categories c
        WHERE c.id = :id
        """,
    )
    abstract fun observeByIdWithCount(id: Long): Flow<CategoryRow?>

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.sortOrder AS sortOrder,
               (SELECT COUNT(*) FROM questions q WHERE q.categoryId = c.id) AS questionCount
        FROM categories c
        ORDER BY c.sortOrder ASC, c.id ASC
        """,
    )
    abstract suspend fun allWithCount(): List<CategoryRow>

    @Query("SELECT MAX(sortOrder) FROM categories")
    abstract suspend fun maxSortOrder(): Int?

    @Insert
    abstract suspend fun insert(entity: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    /** 分类删除前置空：题目落到「未分类」而不是被连带删除 */
    @Query("UPDATE questions SET categoryId = NULL WHERE categoryId = :id")
    abstract suspend fun detachCategory(id: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    /**
     * 事务内先摘除题目再删分类（外键 SET_NULL 双保险）。
     * 必须 open：Kotlin 默认 final，Room 无法为非 final 的 @Transaction 方法生成包装。
     */
    @Transaction
    open suspend fun deleteAndDetach(id: Long) {
        detachCategory(id)
        deleteById(id)
    }
}
