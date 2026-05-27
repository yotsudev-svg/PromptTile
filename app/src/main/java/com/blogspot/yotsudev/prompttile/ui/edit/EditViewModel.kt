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
    val wordsInDisabledOnly: List<PromptWordWithCategory> = emptyList(),
    val filterMode: ManagementFilterMode = ManagementFilterMode.ALL,
)

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: PromptRepository,
    private val dataSource: PreferencesDataSource,
) : ViewModel() {

    private val _expandedCategoryId = MutableStateFlow<Long?>(null)
    
    private val _prefs = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(),
    )

    private val _filterMode = combine(_prefs) { it[0].managementFilterMode }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _wordsInExpanded = _expandedCategoryId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) 
        else repository.allWordsByCategory(id).map { words ->
            words.sortedBy { it.isHidden }
        }
    }

    private val _disabledOnlyWords = repository.allWordsWithCategory.map { list ->
        list.filter { it.word.isHidden }
    }

    val uiState = combine(
        repository.allCategories,
        _expandedCategoryId,
        _wordsInExpanded,
        _disabledOnlyWords,
        _filterMode
    ) { cats, expId, words, disabledWords, mode ->
        val filteredCats = when (mode) {
            ManagementFilterMode.ALL -> cats.sortedBy { it.isHidden }
            ManagementFilterMode.ENABLED_ONLY -> cats.filter { !it.isHidden }
            ManagementFilterMode.DISABLED_ONLY -> cats.filter { it.isHidden }
        }
        EditUiState(
            categories = filteredCats,
            expandedCategoryId = expId,
            wordsInExpanded = if (mode == ManagementFilterMode.ENABLED_ONLY) words.filter { !it.isHidden } else words,
            wordsInDisabledOnly = disabledWords,
            filterMode = mode
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
    }

    // ---- カテゴリ操作 ----------------------------------------

    fun addCategory(ja: String, en: String) {
        viewModelScope.launch { repository.insertCategory(CategoryEntity(nameJa = ja, nameEn = en)) }
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
        // メモリ上の並び替えロジック
    }

    fun persistCategoryOrder() {
        viewModelScope.launch {
            val updated = uiState.value.categories.mapIndexed { i, c -> c.copy(sortOrder = i) }
            repository.updateCategories(updated)
        }
    }

    fun resetCategoryOrder() {
        viewModelScope.launch { repository.resetCategoryOrder() }
    }

    // ---- 単語操作 --------------------------------------------

    fun addWord(categoryId: Long, en: String, ja: String) {
        viewModelScope.launch { repository.insertWord(PromptWordEntity(categoryId = categoryId, wordEn = en, wordJa = ja)) }
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

    fun reorderWords(from: Int, to: Int) {}
    fun persistWordOrder() {}
}
