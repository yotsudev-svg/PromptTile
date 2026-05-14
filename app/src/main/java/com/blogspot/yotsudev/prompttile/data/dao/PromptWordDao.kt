package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptWordDao {

    @Query("SELECT * FROM prompt_words WHERE categoryId = :categoryId AND isHidden = 0 ORDER BY sortOrder ASC")
    fun observeVisibleByCategory(categoryId: Long): Flow<List<PromptWordEntity>>

    @Query("SELECT * FROM prompt_words WHERE categoryId = :categoryId ORDER BY sortOrder ASC")
    fun observeAllByCategory(categoryId: Long): Flow<List<PromptWordEntity>>

    /**
     * 指定カテゴリ内の単語英語名のみ取得する。
     * ポジ・ネガ間の重複を許容するため、全単語ではなく
     * カテゴリ単位でチェックするように変更。
     * （例: ポジの未分類に "blurry" があってもネガの未分類に登録できる）
     */
    @Query("SELECT wordEn FROM prompt_words WHERE categoryId = :categoryId")
    suspend fun getWordEnsByCategory(categoryId: Long): List<String>

    @Query("UPDATE prompt_words SET categoryId = :newCategoryId WHERE id = :wordId")
    suspend fun updateCategory(wordId: Long, newCategoryId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<PromptWordEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: PromptWordEntity)

    @Update
    suspend fun update(word: PromptWordEntity)

    @Delete
    suspend fun delete(word: PromptWordEntity)
}