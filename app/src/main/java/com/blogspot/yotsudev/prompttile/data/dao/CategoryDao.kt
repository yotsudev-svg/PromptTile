package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.ParentCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao : BaseDao<CategoryEntity> {

    // ---- Parent Categories ----

    @Query("SELECT * FROM parent_categories ORDER BY sortOrder ASC")
    fun observeParentCategories(): Flow<List<ParentCategoryEntity>>

    @Query("SELECT * FROM parent_categories WHERE isNegative = :isNegative ORDER BY sortOrder ASC")
    fun observeParentCategoriesByMode(isNegative: Boolean): Flow<List<ParentCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParents(parents: List<ParentCategoryEntity>)

    // ---- Child Categories ----

    /**
     * 指定した親カテゴリに属するカテゴリを表示順に監視する。
     */
    @Query("""
        SELECT * FROM categories 
        WHERE (:includeHidden = 1 OR isHidden = 0) 
        AND isNegative = :isNegative 
        AND parentId = :parentId
        ORDER BY sortOrder ASC
    """)
    fun observeCategoriesByParent(
        parentId: Long,
        isNegative: Boolean,
        includeHidden: Boolean = false
    ): Flow<List<CategoryEntity>>

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

    /** カテゴリの並び順をID順（登録順）にリセットする */
    @Query("UPDATE categories SET sortOrder = id")
    suspend fun resetOrderToId()
}