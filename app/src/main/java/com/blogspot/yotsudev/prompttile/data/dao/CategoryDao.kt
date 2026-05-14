package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /**
     * ポジティブ用: 非表示でなく、ネガティブ専用でもないカテゴリを返す。
     */
    @Query("SELECT * FROM categories WHERE isHidden = 0 AND isNegative = 0 ORDER BY sortOrder ASC")
    fun observeVisible(): Flow<List<CategoryEntity>>

    /**
     * ネガティブ用: ネガティブ専用カテゴリのみ返す。
     * isHidden も考慮することで編集画面からの非表示トグルが効く。
     */
    @Query("SELECT * FROM categories WHERE isHidden = 0 AND isNegative = 1 ORDER BY sortOrder ASC")
    fun observeVisibleNegative(): Flow<List<CategoryEntity>>

    /** 全カテゴリを返す（EditScreen用） */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    /**
     * 未分類カテゴリを nameEn で取得する。
     * IDをハードコードせず名前で引くことで、
     * カテゴリが増えてIDがずれても正しく動作する。
     */
    @Query("SELECT * FROM categories WHERE nameEn = :nameEn LIMIT 1")
    suspend fun getCategoryByNameEn(nameEn: String): CategoryEntity?
}