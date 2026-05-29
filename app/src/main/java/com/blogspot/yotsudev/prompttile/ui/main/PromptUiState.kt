package com.blogspot.yotsudev.prompttile.ui.main

import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
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
    val allTemplates: List<SavedPromptEntity> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val positivePromptText: String = "",
    val negativePromptText: String = "",
    val searchQuery: String = "",
    val searchResults: List<PromptWordEntity> = emptyList(),

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
) {
    /**
     * プロンプト文字列として出力するベース単語。
     * トッピング選択時は "red silk dress with ribbon" のように結合する。
     */
    val baseText: String
        get() {
            val prefixes = selectedToppings.filter { it.isPrefix }.joinToString(" ") { it.valueEn }
            val suffixes = selectedToppings.filter { !it.isPrefix }.joinToString(" ") { it.valueEn }
            
            return listOfNotNull(
                prefixes.takeIf { it.isNotBlank() },
                wordEn,
                suffixes.takeIf { it.isNotBlank() }
            ).joinToString(" ")
        }

    val formatted: String
        get() = com.blogspot.yotsudev.prompttile.util.PromptFormatter.formatItem(this)
}

data class SelectedTopping(
    val groupId: Long,
    val valueEn: String,
    val isPrefix: Boolean,
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
