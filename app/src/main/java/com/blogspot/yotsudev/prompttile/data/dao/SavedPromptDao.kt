package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPromptDao : BaseDao<SavedPromptEntity> {

    /** 保存済みプロンプトを新しい順に監視する */
    @Query("SELECT * FROM saved_prompts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedPromptEntity>>
}