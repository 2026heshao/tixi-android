package com.tixi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tixi.app.data.local.dao.CategoryDao
import com.tixi.app.data.local.dao.ImportDao
import com.tixi.app.data.local.dao.PracticeDao
import com.tixi.app.data.local.dao.QuestionDao
import com.tixi.app.data.local.dao.QuestionTypeDao
import com.tixi.app.data.local.entity.CategoryEntity
import com.tixi.app.data.local.entity.DraftQuestionEntity
import com.tixi.app.data.local.entity.ImportDraftEntity
import com.tixi.app.data.local.entity.ImportRecordEntity
import com.tixi.app.data.local.entity.PracticeProgressEntity
import com.tixi.app.data.local.entity.QuestionEntity
import com.tixi.app.data.local.entity.QuestionTypeEntity
import com.tixi.app.domain.model.BuiltInQuestionType

@Database(
    entities = [
        CategoryEntity::class,
        QuestionTypeEntity::class,
        QuestionEntity::class,
        ImportDraftEntity::class,
        DraftQuestionEntity::class,
        ImportRecordEntity::class,
        PracticeProgressEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TixiDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun questionDao(): QuestionDao
    abstract fun questionTypeDao(): QuestionTypeDao
    abstract fun importDao(): ImportDao
    abstract fun practiceDao(): PracticeDao

    companion object {
        const val DB_NAME = "tixi.db"

        /**
         * 建库。
         *
         * ⚠️ 这里**不能**再调用 `.addTypeConverter(...)`：实体的富内容列（options/answer/
         * richBlocks/tags/qualityFlags）在库里一律是 TEXT，由 Mappers 负责 JSON 编解码，
         * 所以 Room 生成的 `getRequiredConverters()` 全是空列表。此时多传一个转换器实例，
         * `RoomDatabase.init()` 会判定「未被任何 DAO 使用」并抛
         * `IllegalArgumentException: Unexpected type converter`，导致首次查库必崩。
         */
        fun create(context: Context): TixiDatabase =
            Room.databaseBuilder(context, TixiDatabase::class.java, DB_NAME)
                .addCallback(SeedCallback)
                .build()

        /** 首建库写入三种内置题型（选题/填空/大题），sortOrder 0/1/2 */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                BuiltInQuestionType.entries.forEachIndexed { index, type ->
                    db.execSQL(
                        // `key` 是 SQLite 保留字，必须加反引号
                        "INSERT INTO question_types (`key`, name, builtIn, sortOrder) VALUES (?, ?, 1, ?)",
                        arrayOf(type.key, type.label, index),
                    )
                }
            }
        }
    }
}
