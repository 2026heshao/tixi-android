package com.tixi.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.tixi.app.data.local.TixiDatabase
import com.tixi.app.data.local.dao.CategoryDao
import com.tixi.app.data.local.dao.ImportDao
import com.tixi.app.data.local.dao.PracticeDao
import com.tixi.app.data.local.dao.QuestionDao
import com.tixi.app.data.local.dao.QuestionTypeDao
import com.tixi.app.data.repository.CategoryRepositoryImpl
import com.tixi.app.data.repository.ImportRepositoryImpl
import com.tixi.app.data.repository.PracticeRepositoryImpl
import com.tixi.app.data.repository.QuestionRepositoryImpl
import com.tixi.app.data.repository.QuestionTypeRepositoryImpl
import com.tixi.app.data.preferences.SettingsDataStore
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.ImportRepository
import com.tixi.app.domain.repository.PracticeRepository
import com.tixi.app.domain.repository.QuestionRepository
import com.tixi.app.domain.repository.QuestionTypeRepository
import com.tixi.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/** 设置项本地文件名；Context 扩展属性按 DataStore 要求全局唯一 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tixi_settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindQuestionRepository(impl: QuestionRepositoryImpl): QuestionRepository

    @Binds
    @Singleton
    abstract fun bindQuestionTypeRepository(impl: QuestionTypeRepositoryImpl): QuestionTypeRepository

    @Binds
    @Singleton
    abstract fun bindImportRepository(impl: ImportRepositoryImpl): ImportRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPracticeRepository(impl: PracticeRepositoryImpl): PracticeRepository

    companion object {

        /** 全应用共用：多态字段判别键固定为 type，解析时忽略未知字段（前后端版本松耦合） */
        @Provides
        @Singleton
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
        }

        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): TixiDatabase = TixiDatabase.create(context)

        @Provides
        fun provideCategoryDao(db: TixiDatabase): CategoryDao = db.categoryDao()

        @Provides
        fun provideQuestionDao(db: TixiDatabase): QuestionDao = db.questionDao()

        @Provides
        fun provideQuestionTypeDao(db: TixiDatabase): QuestionTypeDao = db.questionTypeDao()

        @Provides
        fun provideImportDao(db: TixiDatabase): ImportDao = db.importDao()

        @Provides
        fun providePracticeDao(db: TixiDatabase): PracticeDao = db.practiceDao()

        @Provides
        @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.settingsDataStore
    }
}
