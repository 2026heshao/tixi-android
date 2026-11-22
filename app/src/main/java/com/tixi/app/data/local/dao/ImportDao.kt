package com.tixi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.tixi.app.data.local.entity.DraftQuestionEntity
import com.tixi.app.data.local.entity.ImportDraftEntity
import com.tixi.app.data.local.entity.ImportRecordEntity
import kotlinx.coroutines.flow.Flow

/** 草稿批次 + 逐题，供确认页一次读全 */
data class ImportDraftWithQuestions(
    @Embedded val draft: ImportDraftEntity,
    @Relation(parentColumn = "id", entityColumn = "draftId")
    val questions: List<DraftQuestionEntity>,
)

@Dao
interface ImportDao {

    // ---- 导入记录 ----

    @Query("SELECT * FROM import_records ORDER BY createdAt DESC, id DESC")
    fun observeRecords(): Flow<List<ImportRecordEntity>>

    @Insert
    suspend fun insertRecord(entity: ImportRecordEntity): Long

    @Query("DELETE FROM import_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    // ---- 草稿批次 ----

    @Insert
    suspend fun insertDraft(entity: ImportDraftEntity): Long

    @Insert
    suspend fun insertDraftQuestions(questions: List<DraftQuestionEntity>): List<Long>

    @Transaction
    @Query("SELECT * FROM import_drafts WHERE id = :draftId")
    fun observeDraftWithQuestions(draftId: Long): Flow<ImportDraftWithQuestions?>

    @Transaction
    @Query("SELECT * FROM import_drafts WHERE id = :draftId")
    suspend fun getDraftWithQuestions(draftId: Long): ImportDraftWithQuestions?

    @Query("SELECT * FROM draft_questions WHERE draftId = :draftId ORDER BY id ASC")
    suspend fun draftQuestions(draftId: Long): List<DraftQuestionEntity>

    @Update
    suspend fun updateDraftQuestion(entity: DraftQuestionEntity)

    @Query("UPDATE draft_questions SET confirmed = :confirmed WHERE id = :id")
    suspend fun setConfirmed(id: Long, confirmed: Boolean)

    @Query("SELECT COUNT(*) FROM draft_questions WHERE draftId = :draftId AND confirmed = 0")
    suspend fun countUnconfirmed(draftId: Long): Int

    @Query("UPDATE import_drafts SET status = :status WHERE id = :draftId")
    suspend fun updateDraftStatus(draftId: Long, status: String)

    /** 外键 CASCADE 会连带删除 draft_questions */
    @Query("DELETE FROM import_drafts WHERE id = :draftId")
    suspend fun deleteDraft(draftId: Long)
}
