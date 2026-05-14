package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val expandedCategoryId: Long? = null,
    val wordsInExpanded: List<PromptWordEntity> = emptyList(),
)

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: PromptRepository,
) : ViewModel() {

    private val _expandedCategoryId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _wordsInExpanded = _expandedCategoryId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.allWordsByCategory(id)
    }

    val uiState = combine(
        repository.allCategories,
        _expandedCategoryId,
        _wordsInExpanded,
    ) { categories, expandedId, words ->
        EditUiState(
            categories = categories,
            expandedCategoryId = expandedId,
            wordsInExpanded = words,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditUiState(),
    )

    fun toggleExpand(categoryId: Long) {
        _expandedCategoryId.value =
            if (_expandedCategoryId.value == categoryId) null else categoryId
    }

    // ---- カテゴリ CRUD ----

    fun addCategory(nameJa: String, nameEn: String) {
        viewModelScope.launch {
            val nextOrder = uiState.value.categories.size
            repository.insertCategory(
                CategoryEntity(nameJa = nameJa, nameEn = nameEn, sortOrder = nextOrder)
            )
        }
    }

    fun updateCategory(entity: CategoryEntity, nameJa: String, nameEn: String) {
        viewModelScope.launch {
            repository.updateCategory(entity.copy(nameJa = nameJa, nameEn = nameEn))
        }
    }

    fun deleteCategory(entity: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(entity) }
    }

    fun toggleCategoryVisibility(entity: CategoryEntity) {
        viewModelScope.launch { repository.toggleCategoryVisibility(entity) }
    }

    // ---- 単語 CRUD ----

    fun addWord(categoryId: Long, wordEn: String, wordJa: String) {
        viewModelScope.launch {
            val nextOrder = uiState.value.wordsInExpanded.size
            repository.insertWord(
                PromptWordEntity(
                    categoryId = categoryId,
                    wordEn = wordEn,
                    wordJa = wordJa,
                    sortOrder = nextOrder,
                )
            )
        }
    }

    /**
     * 単語の内容を更新し、必要であればカテゴリも移動する。
     *
     * [newCategoryId] が null または現在と同じ場合はカテゴリ移動をスキップする。
     * updateWord と moveWordToCategory を別々に呼ぶことで、
     * 「内容だけ変更」「カテゴリだけ移動」「両方変更」の
     * 3ケースをすべて同じメソッドで処理できる。
     */
    fun updateWord(entity: PromptWordEntity, wordEn: String, wordJa: String, newCategoryId: Long?) {
        viewModelScope.launch {
            repository.updateWord(entity.copy(wordEn = wordEn, wordJa = wordJa))
            if (newCategoryId != null && newCategoryId != entity.categoryId) {
                repository.moveWordToCategory(entity.id, newCategoryId)
            }
        }
    }

    fun deleteWord(entity: PromptWordEntity) {
        viewModelScope.launch { repository.deleteWord(entity) }
    }

    fun toggleWordVisibility(entity: PromptWordEntity) {
        viewModelScope.launch { repository.toggleWordVisibility(entity) }
    }
}