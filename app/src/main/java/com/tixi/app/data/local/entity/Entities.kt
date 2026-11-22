package com.tixi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 本地库表结构。JSON 列（optionsJson / answerJson / richBlocksJson / tagsJson / qualityFlagsJson）
 * 由 [com.tixi.app.data.local.Converters] 与 [com.tixi.app.data.local.Mappers] 负责编解码，
 * 解码失败一律回落空值，不允许因为脏数据崩溃。
 */

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "question_types", indices = [Index(value = ["key"], unique = true)])
data class QuestionTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val name: String,
    val builtIn: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["categoryId", "sortOrder"])],
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val stem: String,
    val optionsJson: String? = null,
    val answerJson: String,
    val explanation: String? = null,
    val questionType: String = "choice",
    val richBlocksJson: String? = null,
    val sortOrder: Int = 0,
    val sourceFile: String? = null,
    val sourceImagePath: String? = null,
    val tagsJson: String? = null,
    val remark: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "import_drafts")
data class ImportDraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceFileName: String,
    val sourceFilePath: String,
    val rawResultPath: String? = null,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "draft_questions",
    foreignKeys = [
        ForeignKey(
            entity = ImportDraftEntity::class,
            parentColumns = ["id"],
            childColumns = ["draftId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["draftId"])],
)
data class DraftQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val draftId: Long,
    val stem: String,
    val optionsJson: String? = null,
    val answerJson: String? = null,
    val explanation: String? = null,
    val questionType: String = "choice",
    val richBlocksJson: String? = null,
    val suggestedCategoryId: Long? = null,
    val qualityFlagsJson: String? = null,
    val needsReview: Boolean = false,
    val confirmed: Boolean = false,
)

@Entity(tableName = "import_records")
data class ImportRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceFileName: String,
    val sourceType: String,
    val importedCount: Int = 0,
    val status: String,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "practice_progress", primaryKeys = ["categoryId"])
data class PracticeProgressEntity(
    val categoryId: Long,
    val lastQuestionId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
