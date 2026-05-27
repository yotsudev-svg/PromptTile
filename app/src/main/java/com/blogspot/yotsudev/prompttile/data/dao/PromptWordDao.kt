package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptWordDao : BaseDao<PromptWordEntity> {

    /** 指定カテゴリ内の単語を表示順（sortOrder）に監視する */
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

    /** 全ての単語をカテゴリ情報付きで取得する */
    @Query("""
        SELECT w.*, c.nameJa AS categoryNameJa, c.nameEn AS categoryNameEn
        FROM prompt_words w
        INNER JOIN categories c ON w.categoryId = c.id
        ORDER BY c.sortOrder ASC, w.sortOrder ASC
    """)
    fun observeAllWithCategory(): Flow<List<PromptWordWithCategory>>

    @Query("SELECT wordEn FROM prompt_words WHERE categoryId = :categoryId")
    suspend fun getWordEnsByCategory(categoryId: Long): List<String>

    @Query("UPDATE prompt_words SET categoryId = :newCategoryId WHERE id = :wordId")
    suspend fun updateCategory(wordId: Long, newCategoryId: Long)

    @Query("""
        SELECT * FROM prompt_words 
        WHERE (wordEn LIKE '%' || :query || '%' OR wordJa LIKE '%' || :query || '%')
        AND isHidden = 0
        LIMIT 100
    """)
    fun searchWords(query: String): Flow<List<PromptWordEntity>>

    /** 指定カテゴリ内の単語の並び順をID順（登録順）に一括リセットする */
    @Query("UPDATE prompt_words SET sortOrder = id WHERE categoryId = :categoryId")
    suspend fun resetOrderToId(categoryId: Long)
}