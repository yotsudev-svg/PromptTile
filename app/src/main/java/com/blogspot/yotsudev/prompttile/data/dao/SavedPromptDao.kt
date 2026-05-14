package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPromptDao {

    /** 新しい順に並べて全件取得 */
    @Query("SELECT * FROM saved_prompts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedPromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: SavedPromptEntity)

    @Delete
    suspend fun delete(prompt: SavedPromptEntity)
}