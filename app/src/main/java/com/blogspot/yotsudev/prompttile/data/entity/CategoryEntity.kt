package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nameJa: String,
    val nameEn: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val isHidden: Boolean = false,
    /**
     * trueのときネガティブプロンプト専用カテゴリ。
     * ポジティブモードでは非表示、ネガティブモードでのみ表示される。
     * 既存カテゴリはすべて false（デフォルト値）なので、
     * マイグレーション時のデフォルト値として安全に使える。
     */
    val isNegative: Boolean = false,
    /**
     * 親カテゴリのID。
     * 1: 基本・画風, 2: キャラクター, 3: 顔・髪, 4: 服装, 5: 環境・構図, 6: その他・特殊
     */
    val parentId: Long = 0,
)