package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordWithCategory
import com.blogspot.yotsudev.prompttile.data.preferences.ManagementFilterMode
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.UserPreferences
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val expandedCategoryId: Long? = null,
    val wordsInExpanded: List<PromptWordEntity> = emptyList(),
    val allDisabledWords: List<PromptWordWithCategory> = emptyList(),
    val filterMode: ManagementFilterMode = ManagementFilterMode.ALL,
    val isDragging: Boolean = false,
)

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: PromptRepository,
    private val dataSource: PreferencesDataSource,
) : ViewModel() {

    private val _expandedCategoryId = MutableStateFlow<Long?>(null)
    private val _isDragging = MutableStateFlow(false)

    private val _prefs = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(),
    )

    private val _filterMode = _prefs.map { it.managementFilterMode }

    // ---- ドラッグ中の一時的な並び替え状態（nullのときはDBの値を優先） ----
    private val _reorderedCategories = MutableStateFlow<List<CategoryEntity>?>(null)
    private val _reorderedWords = MutableStateFlow<List<PromptWordEntity>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _categoriesFlow = combine(
        repository.allCategories,
        _filterMode,
        _reorderedCategories
    ) { cats, mode, reordered ->
        if (reordered != null) return@combine reordered

        when (mode) {
            ManagementFilterMode.ALL -> cats
            ManagementFilterMode.ENABLED_ONLY -> cats.filter { !it.isHidden }
            ManagementFilterMode.DISABLED_ONLY -> cats.filter { it.isHidden }
            else -> cats
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _wordsInExpanded = combine(
        _expandedCategoryId,
        _filterMode,
        _reorderedWords
    ) { expId, mode, reordered ->
        if (expId == null) return@combine flowOf(emptyList<PromptWordEntity>())
        if (reordered != null) return@combine flowOf(reordered)

        repository.allWordsByCategory(expId).map { words ->
            when (mode) {
                ManagementFilterMode.ALL -> words
                ManagementFilterMode.ENABLED_ONLY -> words.filter { !it.isHidden }
                ManagementFilterMode.DISABLED_ONLY -> words.filter { it.isHidden }
                else -> words
            }
        }
    }.flatMapLatest { it }

    private val _allDisabledWords = repository.allWordsWithCategory.map { list ->
        list.filter { it.word.isHidden }
    }

    val uiState = combine(
        _categoriesFlow,
        _expandedCategoryId,
        _wordsInExpanded,
        _allDisabledWords,
        _filterMode,
        _isDragging,
    ) { args: Array<Any?> ->
        EditUiState(
            categories = args[0] as List<CategoryEntity>,
            expandedCategoryId = args[1] as Long?,
            wordsInExpanded = args[2] as List<PromptWordEntity>,
            allDisabledWords = args[3] as List<PromptWordWithCategory>,
            filterMode = args[4] as ManagementFilterMode,
            isDragging = args[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditUiState(),
    )

    fun setFilterMode(mode: ManagementFilterMode) {
        viewModelScope.launch { dataSource.updateManagementFilterMode(mode) }
    }

    fun toggleExpand(categoryId: Long) {
        _expandedCategoryId.update { if (it == categoryId) null else categoryId }
        _reorderedWords.value = null
    }

    fun setIsDragging(dragging: Boolean) {
        _isDragging.value = dragging
    }

    // ---- カテゴリ操作 ----------------------------------------

    fun addCategory(ja: String, en: String) {
        viewModelScope.launch {
            val maxSortOrder = uiState.value.categories.maxOfOrNull { it.sortOrder } ?: 0
            repository.insertCategory(CategoryEntity(nameJa = ja, nameEn = en, sortOrder = maxSortOrder + 1))
        }
    }

    fun updateCategory(category: CategoryEntity, ja: String, en: String) {
        viewModelScope.launch { repository.updateCategory(category.copy(nameJa = ja, nameEn = en)) }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun toggleCategoryVisibility(category: CategoryEntity) {
        viewModelScope.launch { repository.toggleCategoryVisibility(category) }
    }

    fun reorderCategories(from: Int, to: Int) {
        val current = uiState.value.categories.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _reorderedCategories.value = current
    }

    fun persistCategoryOrder() {
        val list = _reorderedCategories.value ?: return
        viewModelScope.launch {
            val updated = list.mapIndexed { i, c -> c.copy(sortOrder = i) }
            repository.updateCategories(updated)
            _reorderedCategories.value = null
            _isDragging.value = false // ドラッグ終了を明示
        }
    }

    fun resetOrder() {
        viewModelScope.launch {
            val expId = _expandedCategoryId.value
            if (expId != null) {
                repository.resetWordOrder(expId)
            } else {
                repository.resetCategoryOrder()
            }
        }
    }

    // ---- 単語操作 --------------------------------------------

    fun addWord(categoryId: Long, en: String, ja: String) {
        viewModelScope.launch {
            val maxSortOrder = uiState.value.wordsInExpanded.maxOfOrNull { it.sortOrder } ?: 0
            repository.insertWord(PromptWordEntity(categoryId = categoryId, wordEn = en, wordJa = ja, sortOrder = maxSortOrder + 1))
        }
    }

    fun updateWord(word: PromptWordEntity, en: String, ja: String, newCategoryId: Long?) {
        viewModelScope.launch {
            val updated = word.copy(wordEn = en, wordJa = ja, categoryId = newCategoryId ?: word.categoryId)
            repository.updateWord(updated)
        }
    }

    fun deleteWord(word: PromptWordEntity) {
        viewModelScope.launch { repository.deleteWord(word) }
    }

    fun toggleWordVisibility(word: PromptWordEntity) {
        viewModelScope.launch { repository.toggleWordVisibility(word) }
    }

    fun reorderWords(from: Int, to: Int) {
        val current = uiState.value.wordsInExpanded.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _reorderedWords.value = current
    }

    fun persistWordOrder() {
        val list = _reorderedWords.value ?: return
        viewModelScope.launch {
            val updated = list.mapIndexed { i, w -> w.copy(sortOrder = i) }
            repository.updateWords(updated)
            _reorderedWords.value = null
            _isDragging.value = false // ドラッグ終了を明示
        }
    }
}
