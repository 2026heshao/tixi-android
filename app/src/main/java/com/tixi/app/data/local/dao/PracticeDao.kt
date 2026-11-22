package com.tixi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tixi.app.data.local.entity.PracticeProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {

    @Query("SELECT * FROM practice_progress WHERE categoryId = :categoryId")
    fun observeProgress(categoryId: Long): Flow<PracticeProgressEntity?>

    @Query("SELECT * FROM practice_progress WHERE categoryId = :categoryId")
    suspend fun findProgress(categoryId: Long): PracticeProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PracticeProgressEntity)
}
