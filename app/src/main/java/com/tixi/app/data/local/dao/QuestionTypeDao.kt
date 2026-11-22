package com.tixi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tixi.app.data.local.entity.QuestionTypeEntity
import kotlinx.coroutines.flow.Flow

/** 题型 + 该题型题目数（子查询合并，未匹配为 0） */
data class QuestionTypeRow(
    val id: Long,
    val typeKey: String,
    val name: String,
    val builtIn: Boolean,
    val sortOrder: Int,
    val questionCount: Int,
)

@Dao
interface QuestionTypeDao {

    // `key` 在 SQLite 里是保留字（PRIMARY KEY），列名必须用反引号包起来
    @Query(
        """
        SELECT t.id AS id, t.`key` AS typeKey, t.name AS name, t.builtIn AS builtIn, t.sortOrder AS sortOrder,
               (SELECT COUNT(*) FROM questions q WHERE q.questionType = t.`key`) AS questionCount
        FROM question_types t
        ORDER BY t.sortOrder ASC, t.id ASC
        """,
    )
    fun observeAllWithCount(): Flow<List<QuestionTypeRow>>

    @Query(
        """
        SELECT t.id AS id, t.`key` AS typeKey, t.name AS name, t.builtIn AS builtIn, t.sortOrder AS sortOrder,
               (SELECT COUNT(*) FROM questions q WHERE q.questionType = t.`key`) AS questionCount
        FROM question_types t
        ORDER BY t.sortOrder ASC, t.id ASC
        """,
    )
    suspend fun allWithCount(): List<QuestionTypeRow>

    @Query("SELECT * FROM question_types WHERE `key` = :key")
    suspend fun findByKey(key: String): QuestionTypeEntity?

    @Query("SELECT * FROM question_types WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): QuestionTypeEntity?

    @Query("SELECT MAX(sortOrder) FROM question_types")
    suspend fun maxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM question_types WHERE builtIn = 0")
    suspend fun countCustom(): Int

    @Insert
    suspend fun insert(entity: QuestionTypeEntity): Long

    @Query("UPDATE question_types SET name = :name WHERE `key` = :key")
    suspend fun updateName(key: String, name: String)

    @Query("DELETE FROM question_types WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
