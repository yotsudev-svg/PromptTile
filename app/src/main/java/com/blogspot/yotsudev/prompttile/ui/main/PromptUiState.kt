package com.blogspot.yotsudev.prompttile.ui.main

import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity
import com.blogspot.yotsudev.prompttile.data.seed.PrefixTemplate

enum class PromptMode { POSITIVE, NEGATIVE }

data class PromptUiState(
    val mode: PromptMode = PromptMode.POSITIVE,

    val positiveItems: List<PromptItem> = emptyList(),
    val positiveCategories: List<CategoryEntity> = emptyList(),
    val selectedPositiveCategoryId: Long? = null,

    val negativeItems: List<PromptItem> = emptyList(),
    val negativeCategories: List<CategoryEntity> = emptyList(),
    val selectedNegativeCategoryId: Long? = null,

    val wordsInCategory: List<PromptWordEntity> = emptyList(),
    val isLoading: Boolean = true,
    val moveToBackOnCopy: Boolean = false,
    val allTemplates: List<PrefixTemplate> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val positivePromptText: String = "",
    val negativePromptText: String = "",
    val searchQuery: String = "",
    val searchResults: List<PromptWordEntity> = emptyList(),

    // ---- マルチ調整ボトムシート ----
    /** 現在シートで編集中の PromptItem。null のときシートは非表示 */
    val adjustingItem: PromptItem? = null,
    /** adjustingItem に対応するトッピング選択肢。トッピング非対応なら空リスト */
    val adjustingToppingItems: List<ToppingItemEntity> = emptyList(),
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
    /**
     * 紐づくトッピンググループID。
     * DB の PromptWordEntity.toppingGroupId を起点に
     * WordPool → ViewModel → PromptItem と伝播させる。
     * null のとき通常単語（マルチ調整シートにトッピング欄を表示しない）。
     */
    val toppingGroupId: Long? = null,
    /**
     * 現在選択中のトッピング文字列（例: "red"）。
     * null のときはトッピング未選択 → プロンプトには wordEn のみ出力。
     * 選択済みのとき → "${topping} ${wordEn}" の形で出力。
     */
    val selectedTopping: String? = null,
) {
    /**
     * プロンプト文字列として出力するベース単語。
     * トッピング選択時は "red swimsuit" のように結合する。
     */
    val baseText: String
        get() = if (selectedTopping != null) "$selectedTopping $wordEn" else wordEn

    val formatted: String
        get() = when {
            weight == null || weight == 1.0f -> baseText
            else -> "(${baseText}:${String.format("%.1f", weight)})"
        }
}

data class ClipboardImportItem(
    val id: Int,
    val wordEn: String,
    val isEnabled: Boolean = true,
    val registerToDb: Boolean = false,
)