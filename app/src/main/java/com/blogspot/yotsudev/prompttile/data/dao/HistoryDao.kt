package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao : BaseDao<HistoryEntity> {
    @Query("SELECT * FROM copy_histories ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT COUNT(*) FROM copy_histories")
    suspend fun getCount(): Int

    @Query("DELETE FROM copy_histories WHERE id IN (SELECT id FROM copy_histories ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM copy_histories")
    suspend fun deleteAll()
}
