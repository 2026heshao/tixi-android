package com.tixi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tixi.app.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

/** countByCategory 的投影：categoryId 为 null 表示「未分类」 */
data class CategoryCount(val categoryId: Long?, val cnt: Int)

/** countByType 的投影 */
data class TypeCount(val questionType: String, val cnt: Int)

@Dao
abstract class QuestionDao {

    @Query("SELECT * FROM questions ORDER BY createdAt DESC, id DESC")
    abstract fun observeAll(): Flow<List<QuestionEntity>>

    /**
     * categoryId = null 表示「未分类」，即 categoryId IS NULL 的题目
     * （与 [search] 的 null 语义不同，见 search 注释）。
     */
    @Query(
        """
        SELECT * FROM questions
        WHERE (:categoryId IS NULL AND categoryId IS NULL) OR categoryId = :categoryId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    abstract fun observeByCategory(categoryId: Long?): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    abstract fun observeById(id: Long): Flow<QuestionEntity?>

    @Query("SELECT * FROM questions WHERE id = :id")
    abstract suspend fun findById(id: Long): QuestionEntity?

    @Query(
        """
        SELECT * FROM questions
        WHERE (:categoryId IS NULL AND categoryId IS NULL) OR categoryId = :categoryId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    abstract suspend fun byCategory(categoryId: Long?): List<QuestionEntity>

    @Insert
    abstract suspend fun insert(entity: QuestionEntity): Long

    @Upsert
    abstract suspend fun upsert(entity: QuestionEntity): Long

    @Query("DELETE FROM questions WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM questions")
    abstract suspend fun count(): Int

    @Query("SELECT categoryId AS categoryId, COUNT(*) AS cnt FROM questions GROUP BY categoryId")
    abstract suspend fun countByCategory(): List<CategoryCount>

    @Query("SELECT questionType AS questionType, COUNT(*) AS cnt FROM questions GROUP BY questionType")
    abstract suspend fun countByType(): List<TypeCount>

    /**
     * 检索。注意 categoryId 语义与 [observeByCategory] 相反：
     * 这里 categoryId = null 表示「不按分类筛选」（全库搜索），不是「只看未分类」。
     * query 为空时只按分类/标签筛选；tagsJson LIKE 做包含匹配。
     */
    @Query(
        """
        SELECT * FROM questions
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:query = '' OR stem LIKE '%' || :query || '%'
               OR IFNULL(tagsJson, '') LIKE '%' || :query || '%')
          AND (:tag IS NULL OR :tag = '' OR IFNULL(tagsJson, '') LIKE '%' || :tag || '%')
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    abstract suspend fun search(query: String, categoryId: Long?, tag: String?): List<QuestionEntity>

    @Query(
        """
        SELECT MAX(sortOrder) FROM questions
        WHERE (:categoryId IS NULL AND categoryId IS NULL) OR categoryId = :categoryId
        """,
    )
    abstract suspend fun maxSortOrder(categoryId: Long?): Int?

    @Query("UPDATE questions SET sortOrder = :order WHERE id = :id")
    abstract suspend fun updateSortOrder(id: Long, order: Int)

    /** 分类内重排：逐个 id 重写 sortOrder，整体包在一个事务里 */
    @Transaction
    open suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updateSortOrder(id, index) }
    }
}
