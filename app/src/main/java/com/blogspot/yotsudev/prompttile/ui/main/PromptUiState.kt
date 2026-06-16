package com.blogspot.yotsudev.prompttile.ui.main

import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.HistoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.ParentCategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingGroupEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity

enum class PromptMode { POSITIVE, NEGATIVE }

data class PromptUiState(
    val mode: PromptMode = PromptMode.POSITIVE,

    val positiveItems: List<PromptItem> = emptyList(),
    val positiveParentCategories: List<ParentCategoryEntity> = emptyList(),
    val selectedPositiveParentId: Long? = null,
    val positiveCategories: List<CategoryEntity> = emptyList(),
    val selectedPositiveCategoryId: Long? = null,

    val negativeItems: List<PromptItem> = emptyList(),
    val negativeParentCategories: List<ParentCategoryEntity> = emptyList(),
    val selectedNegativeParentId: Long? = null,
    val negativeCategories: List<CategoryEntity> = emptyList(),
    val selectedNegativeCategoryId: Long? = null,

    val wordsInCategory: List<PromptWordEntity> = emptyList(),
    val isLoading: Boolean = true,
    val moveToBackOnCopy: Boolean = false,
    val gridColumnsConfig: com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig = com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig.AUTO,
    val allTemplates: List<SavedPromptEntity> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val positivePromptText: String = "",
    val negativePromptText: String = "",
    val searchQuery: String = "",
    val searchResults: List<PromptWordEntity> = emptyList(),

    // ---- コピー履歴 ----
    val copyHistories: List<HistoryEntity> = emptyList(),

    // ---- マルチ調整ボトムシート ----
    /** 現在シートで編集中の PromptItem。null のときシートは非表示 */
    val adjustingItem: PromptItem? = null,
    /** adjustingItem に対応するトッピンググループとその選択肢のリスト */
    val adjustingToppingGroups: List<ToppingGroupWithItems> = emptyList(),
) {
    val currentItems: List<PromptItem>
        get() = if (mode == PromptMode.POSITIVE) positiveItems else negativeItems

    val currentPromptText: String
        get() = if (mode == PromptMode.POSITIVE) positivePromptText else negativePromptText

    val currentParentCategories: List<ParentCategoryEntity>
        get() = if (mode == PromptMode.POSITIVE) positiveParentCategories else negativeParentCategories

    val currentSelectedParentId: Long?
        get() = if (mode == PromptMode.POSITIVE) selectedPositiveParentId else selectedNegativeParentId

    val currentCategories: List<CategoryEntity>
        get() = if (mode == PromptMode.POSITIVE) positiveCategories else negativeCategories

    val currentSelectedCategoryId: Long?
        get() = if (mode == PromptMode.POSITIVE) selectedPositiveCategoryId else selectedNegativeCategoryId
}

data class ToppingConfiguration(
    val toppingGroupIds: List<Long> = emptyList(),
    val excludeToppingValues: Set<String> = emptySet()
)

data class PromptItem(
    val wordId: Long,
    val wordEn: String,
    val wordJa: String,
    val weight: Float? = null,
    /**
     * 紐づくトッピンググループIDのリスト。
     */
    val toppingGroupIds: List<Long> = emptyList(),
    /**
     * 現在選択中のトッピングリスト。
     */
    val selectedToppings: List<SelectedTopping> = emptyList(),
    /**
     * 除外したいトッピングアイテムの valueEn リスト。
     */
    val excludeToppingValues: List<String> = emptyList(),
    /**
     * 単語に付与されたタグ。カンマ区切り。
     */
    val tags: String? = null,
    /**
     * プロンプト生成用のテンプレート（例: "{colorA} to {colorB} gradient hair"）。
     */
    val promptTemplate: String? = null,
) {
    /**
     * プロンプト文字列として出力するベース単語。
     * トッピング選択時は "red silk dress with ribbon" のように結合する。
     */
    val baseText: String
        get() {
            // 特殊髪色/瞳色かつテンプレートがある場合
            val tagList = tags?.split(",")?.map { it.trim() } ?: emptyList()
            if ((tagList.contains("hair_multicolor") || tagList.contains("eye_multicolor")) && promptTemplate != null) {
                val colorA = selectedToppings.find { it.slot == "colorA" }?.valueEn ?: ""
                val colorB = selectedToppings.find { it.slot == "colorB" }?.valueEn ?: ""
                
                return promptTemplate
                    .replace("{colorA}", colorA)
                    .replace("{colorB}", colorB)
                    .trim()
            }

            // Priority-based sorting (lower number comes first)
            val sortedToppings = selectedToppings.sortedBy { it.priority }

            val prefixes = sortedToppings.filter { it.isPrefix }.map { it.valueEn }
            val suffixes = sortedToppings.filter { !it.isPrefix }.map { it.valueEn }

            val isHairColor = tagList.contains("hair_color")
            val isEyeColor = tagList.contains("eye_color")
            val finalWordEn = when {
                isHairColor && prefixes.isNotEmpty() -> "hair"
                isEyeColor && prefixes.isNotEmpty() -> "eyes"
                else -> wordEn
            }

            val mainPart = (prefixes + finalWordEn).filter { it.isNotBlank() }.joinToString(" ")
            return (listOf(mainPart) + suffixes).filter { it.isNotBlank() }.joinToString(", ")
        }

    val formatted: String
        get() = com.blogspot.yotsudev.prompttile.util.PromptFormatter.formatItem(this)
}

data class SelectedTopping(
    val groupId: Long,
    val valueEn: String,
    val isPrefix: Boolean,
    val priority: Int = 999,
    val slot: String? = null,
)

data class ClipboardImportItem(
    val id: Int,
    val wordEn: String,
    val isEnabled: Boolean = true,
    val registerToDb: Boolean = false,
)

data class ToppingGroupWithItems(
    val group: ToppingGroupEntity,
    val items: List<ToppingItemEntity>,
)
