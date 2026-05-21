package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao : BaseDao<CategoryEntity> {

    /**
     * カテゴリを表示順に監視する。
     *
     * @param isNegative ネガティブ用カテゴリを取得する場合は true
     * @param includeHidden 非表示設定のカテゴリも含める場合は true
     */
    @Query("""
        SELECT * FROM categories 
        WHERE (:includeHidden = 1 OR isHidden = 0) 
        AND isNegative = :isNegative 
        ORDER BY sortOrder ASC
    """)
    fun observeCategories(
        isNegative: Boolean,
        includeHidden: Boolean = false
    ): Flow<List<CategoryEntity>>

    /** 全カテゴリを表示順に取得（編集画面用） */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    /**
     * 未分類カテゴリを英語名で取得する。
     */
    @Query("SELECT * FROM categories WHERE nameEn = :nameEn LIMIT 1")
    suspend fun getCategoryByNameEn(nameEn: String): CategoryEntity?
}