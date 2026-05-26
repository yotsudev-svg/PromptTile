package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import com.blogspot.yotsudev.prompttile.util.PromptFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private val _expandedCategoryId = MutableStateFlow<Long?>(null)

    private val _wordsInExpanded = MutableStateFlow<List<PromptWordEntity>>(emptyList())

    init {
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            _expandedCategoryId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList())
                else repository.allWordsByCategory(id)
            }.collect { list ->
                _wordsInExpanded.value = list
            }
        }
    }

    val uiState = combine(
        _categories,
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

    init {
        viewModelScope.launch {
            repository.allCategories.collect { list ->
                // ドラッグ中（一括更新中）は外部からの更新を無視するか、適切にマージする必要があるが、
                // 基本的にDBが真なので、DBからの最新を反映する。
                // ただし、UIスレッドでの並び替えを優先するため、初期ロード時のみ反映する形にする。
                if (_categories.value.isEmpty()) {
                    _categories.value = list
                } else {
                    // 追加や削除があった場合に備えて更新
                    // ただし、単純に代入するとドラッグ中の状態が壊れる可能性があるため、
                    // IDのリストが変わった場合のみ更新する等の工夫が必要。
                    // ここではシンプルに最新を反映（ドラッグ終了時にDBに書き込むので整合性は取れる）
                    _categories.value = list
                }
            }
        }
    }

    fun toggleExpand(categoryId: Long) {
        _expandedCategoryId.update { current ->
            if (current == categoryId) null else categoryId
        }
    }

    // ---- カテゴリ CRUD ----

    fun addCategory(nameJa: String, nameEn: String) {
        viewModelScope.launch {
            val nextOrder = _categories.value.size
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
                    wordEn = PromptFormatter.cleanWord(wordEn),
                    wordJa = wordJa,
                    sortOrder = nextOrder,
                )
            )
        }
    }

    /**
     * 単語の内容を更新し、必要であればカテゴリも移動する。
     */
    fun updateWord(entity: PromptWordEntity, wordEn: String, wordJa: String, newCategoryId: Long?) {
        viewModelScope.launch {
            val updatedEntity = entity.copy(
                wordEn = PromptFormatter.cleanWord(wordEn),
                wordJa = wordJa,
                categoryId = newCategoryId ?: entity.categoryId
            )
            repository.updateWord(updatedEntity)
        }
    }

    fun deleteWord(entity: PromptWordEntity) {
        viewModelScope.launch { repository.deleteWord(entity) }
    }

    fun toggleWordVisibility(entity: PromptWordEntity) {
        viewModelScope.launch { repository.toggleWordVisibility(entity) }
    }

    // ---- 並び替えロジック ----

    /**
     * カテゴリの順序をメモリ内で入れ替える。
     */
    fun reorderCategories(fromIndex: Int, toIndex: Int) {
        val list = _categories.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return

        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)

        _categories.value = list
    }

    /**
     * メモリ内の順序をDBに永続化する。
     */
    fun persistCategoryOrder() {
        val updatedList = _categories.value.mapIndexed { index, category ->
            category.copy(sortOrder = index)
        }
        viewModelScope.launch {
            repository.updateCategories(updatedList)
        }
    }

    /**
     * 現在展開中のカテゴリ内の単語の順序をメモリ内で入れ替える。
     */
    fun reorderWords(fromIndex: Int, toIndex: Int) {
        val list = _wordsInExpanded.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return

        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)

        _wordsInExpanded.value = list
    }

    /**
     * メモリ内の単語の順序をDBに永続化する。
     */
    fun persistWordOrder() {
        val updatedList = _wordsInExpanded.value.mapIndexed { index, word ->
            word.copy(sortOrder = index)
        }
        viewModelScope.launch {
            repository.updateWords(updatedList)
        }
    }

    /**
     * カテゴリの並び順を登録順（ID順）にリセットする。
     */
    fun resetCategoryOrder() {
        viewModelScope.launch {
            repository.resetCategoryOrder()
        }
    }

    /**
     * 指定したカテゴリ内の単語の並び順を登録順（ID順）にリセットする。
     */
    fun resetWordOrder(categoryId: Long) {
        viewModelScope.launch {
            repository.resetWordOrder(categoryId)
        }
    }
}
