package com.blogspot.yotsudev.prompttile.ui.main

import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity

enum class PromptMode { POSITIVE, NEGATIVE }

data class PromptUiState(
    val mode: PromptMode = PromptMode.POSITIVE,

    // ---- ポジティブ側 ----
    val positiveItems: List<PromptItem> = emptyList(),
    val positiveCategories: List<CategoryEntity> = emptyList(),
    val selectedPositiveCategoryId: Long? = null,

    // ---- ネガティブ側 ----
    val negativeItems: List<PromptItem> = emptyList(),
    val negativeCategories: List<CategoryEntity> = emptyList(),
    val selectedNegativeCategoryId: Long? = null,

    // ---- 現在のモードに応じた単語プール ----
    val wordsInCategory: List<PromptWordEntity> = emptyList(),

    val isLoading: Boolean = true,
    val moveToBackOnCopy: Boolean = false,

    // ---- Undo / Redo ----
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // ---- 最終プロンプト文字列（リアクティブ表示用） ----
    val positivePromptText: String = "",
    val negativePromptText: String = "",

    // ---- 検索と最近使った単語 ----
    val searchQuery: String = "",
    val searchResults: List<PromptWordEntity> = emptyList(),
    val recentWords: List<PromptWordEntity> = emptyList(),
) {
    val currentItems: List<PromptItem>
        get() = if (mode == PromptMode.POSITIVE) positiveItems else negativeItems

    val currentPromptText: String
        get() = if (mode == PromptMode.POSITIVE) positivePromptText else negativePromptText

    val currentCategories: List<CategoryEntity>
        get() = if (mode == PromptMode.POSITIVE) positiveCategories else negativeCategories

    val currentSelectedCategoryId: Long?
        get() = if (mode == PromptMode.POSITIVE) selectedPositiveCategoryId else selectedNegativeCategoryId
}

data class PromptItem(
    val wordId: Long,
    val wordEn: String,
    val wordJa: String,
    val weight: Float? = null,
) {
    val formatted: String
        get() = when {
            weight == null || weight == 1.0f -> wordEn
            else -> "(${wordEn}:${String.format("%.1f", weight)})"
        }
}

/**
 * クリップボードインポート用のプレビューアイテム。
 *
 * [isEnabled] false のとき半透明表示で「追加しない」状態を表す。
 * 削除ではなくトグルにすることで「やっぱり追加したい」に対応できる。
 *
 * [registerToDb] true のとき未分類カテゴリへの自動登録対象になる。
 * デフォルト false で「エリアAに一時追加のみ」が基本動作。
 *
 * [id] は UI での状態管理用の一意キー。
 * LazyRow の key に使うことでアニメーションが正確に動作する。
 */
data class ClipboardImportItem(
    val id: Int,
    val wordEn: String,
    val isEnabled: Boolean = true,
    val registerToDb: Boolean = false,
)