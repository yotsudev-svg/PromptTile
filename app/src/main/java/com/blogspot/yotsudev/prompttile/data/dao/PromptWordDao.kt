package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptWordDao : BaseDao<PromptWordEntity> {

    /**
     * 指定カテゴリ内の単語を表示順に監視する。
     *
     * @param includeHidden 非表示設定の単語も含める場合は true
     */
    @Query("""
        SELECT * FROM prompt_words 
        WHERE categoryId = :categoryId 
        AND (:includeHidden = 1 OR isHidden = 0) 
        ORDER BY sortOrder ASC
    """)
    fun observeByCategory(
        categoryId: Long,
        includeHidden: Boolean = false
    ): Flow<List<PromptWordEntity>>

    /**
     * 指定カテゴリ内の単語英語名リストを取得する。
     */
    @Query("SELECT wordEn FROM prompt_words WHERE categoryId = :categoryId")
    suspend fun getWordEnsByCategory(categoryId: Long): List<String>

    /**
     * 単語の所属カテゴリを変更する。
     */
    @Query("UPDATE prompt_words SET categoryId = :newCategoryId WHERE id = :wordId")
    suspend fun updateCategory(wordId: Long, newCategoryId: Long)

    /**
     * 単語名（日英）で検索する。
     */
    @Query("""
        SELECT * FROM prompt_words 
        WHERE (wordEn LIKE '%' || :query || '%' OR wordJa LIKE '%' || :query || '%')
        AND isHidden = 0
        LIMIT 100
    """)
    fun searchWords(query: String): Flow<List<PromptWordEntity>>

    /**
     * IDリストに基づいて単語を取得する。
     */
    @Query("SELECT * FROM prompt_words WHERE id IN (:ids)")
    fun getWordsByIds(ids: List<Long>): Flow<List<PromptWordEntity>>
}