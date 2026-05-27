package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPromptDao : BaseDao<SavedPromptEntity> {

    /** 保存済みプロンプトを表示順に監視する */
    @Query("SELECT * FROM saved_prompts ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<SavedPromptEntity>>

    /** 並び順をID順（登録順）にリセットする */
    @Query("UPDATE saved_prompts SET sortOrder = id")
    suspend fun resetOrderToId()
}