package com.blogspot.yotsudev.prompttile.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.preferences.PersistedPromptItem
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.UserPreferences
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val HISTORY_LIMIT = 10

@HiltViewModel
class PromptViewModel @Inject constructor(
    private val repository: PromptRepository,
    private val dataSource: PreferencesDataSource,
) : ViewModel() {

    private val _mode = MutableStateFlow(PromptMode.POSITIVE)
    private val _positiveItems = MutableStateFlow<List<PromptItem>>(emptyList())
    private val _negativeItems = MutableStateFlow<List<PromptItem>>(emptyList())
    private val _selectedPositiveCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedNegativeCategoryId = MutableStateFlow<Long?>(null)

    private val _positiveUndoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())
    private val _positiveRedoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())
    private val _negativeUndoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())
    private val _negativeRedoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())

    private val _prefs = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _wordsInCategory = _mode.flatMapLatest { mode ->
        val categoryIdFlow = if (mode == PromptMode.POSITIVE)
            _selectedPositiveCategoryId else _selectedNegativeCategoryId
        categoryIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.visibleWordsByCategory(id)
        }
    }

    private data class HistoryState(val canUndo: Boolean, val canRedo: Boolean)

    private val _historyState = combine(
        _mode,
        _positiveUndoStack,
        _positiveRedoStack,
        _negativeUndoStack,
        _negativeRedoStack,
    ) { mode, posUndo, posRedo, negUndo, negRedo ->
        HistoryState(
            canUndo = if (mode == PromptMode.POSITIVE) posUndo.isNotEmpty() else negUndo.isNotEmpty(),
            canRedo = if (mode == PromptMode.POSITIVE) posRedo.isNotEmpty() else negRedo.isNotEmpty(),
        )
    }

    val uiState = combine(
        combine(_mode, _positiveItems, _negativeItems, ::Triple),
        combine(repository.visibleCategories, repository.visibleNegativeCategories, ::Pair),
        combine(_selectedPositiveCategoryId, _selectedNegativeCategoryId, ::Pair),
        _wordsInCategory,
        combine(_prefs, _historyState, ::Pair),
    ) { (mode, posItems, negItems), (posCats, negCats), (posCatId, negCatId), words, (prefs, history) ->
        val resolvedPosCatId = posCatId ?: posCats.firstOrNull()?.id
        val resolvedNegCatId = negCatId ?: negCats.firstOrNull()?.id
        PromptUiState(
            mode                       = mode,
            positiveItems              = posItems,
            positiveCategories         = posCats,
            selectedPositiveCategoryId = resolvedPosCatId,
            negativeItems              = negItems,
            negativeCategories         = negCats,
            selectedNegativeCategoryId = resolvedNegCatId,
            wordsInCategory            = words,
            isLoading                  = false,
            moveToBackOnCopy           = prefs.moveToBackOnCopy,
            canUndo                    = history.canUndo,
            canRedo                    = history.canRedo,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PromptUiState(),
    )

    // ---- 起動時の状態復元 ----------------------------------------

    init {
        viewModelScope.launch {
            val prefs = dataSource.userPreferences.first()
            if (prefs.persistedPositiveItems.isNotEmpty()) {
                _positiveItems.value = prefs.persistedPositiveItems.map { it.toPromptItem() }
            }
            if (prefs.persistedNegativeItems.isNotEmpty()) {
                _negativeItems.value = prefs.persistedNegativeItems.map { it.toPromptItem() }
            }
        }
    }

    // ---- 自動保存ヘルパー ----------------------------------------

    private fun persistItems() {
        viewModelScope.launch {
            dataSource.updatePersistedItems(
                positiveItems = _positiveItems.value.map { it.toPersistedItem() },
                negativeItems = _negativeItems.value.map { it.toPersistedItem() },
            )
        }
    }

    // ---- 履歴ヘルパー -----------------------------------------------

    private fun pushHistory(current: List<PromptItem>) {
        val undoStack = if (_mode.value == PromptMode.POSITIVE) _positiveUndoStack else _negativeUndoStack
        val redoStack = if (_mode.value == PromptMode.POSITIVE) _positiveRedoStack else _negativeRedoStack
        undoStack.update { stack ->
            val next = stack + listOf(current)
            if (next.size > HISTORY_LIMIT) next.drop(1) else next
        }
        redoStack.value = emptyList()
    }

    private val currentItems get() =
        if (_mode.value == PromptMode.POSITIVE) _positiveItems else _negativeItems

    // ---- モード切り替え ----------------------------------------

    fun switchMode(mode: PromptMode) { _mode.value = mode }

    // ---- カテゴリ選択 ------------------------------------------

    fun selectCategory(categoryId: Long) {
        if (_mode.value == PromptMode.POSITIVE) _selectedPositiveCategoryId.value = categoryId
        else _selectedNegativeCategoryId.value = categoryId
    }

    // ---- 単語操作 ------------------------------------------------

    fun toggleWord(word: PromptWordEntity) {
        pushHistory(currentItems.value)
        currentItems.update { current ->
            if (current.any { it.wordId == word.id }) {
                current.filter { it.wordId != word.id }
            } else {
                current + PromptItem(wordId = word.id, wordEn = word.wordEn, wordJa = word.wordJa)
            }
        }
        persistItems()
    }

    fun removeItem(item: PromptItem) {
        pushHistory(currentItems.value)
        currentItems.update { it.filter { i -> i.wordId != item.wordId } }
        persistItems()
    }

    fun cycleWeight(item: PromptItem) {
        pushHistory(currentItems.value)
        val nextWeight = when (item.weight) {
            null -> 1.2f
            1.2f -> 1.5f
            1.5f -> 0.8f
            else -> null
        }
        currentItems.update { current ->
            current.map { if (it.wordId == item.wordId) it.copy(weight = nextWeight) else it }
        }
        persistItems()
    }

    fun moveItem(from: Int, to: Int) {
        currentItems.update { current ->
            current.toMutableList().apply { add(to, removeAt(from)) }
        }
        persistItems()
    }

    fun clearAll() {
        pushHistory(currentItems.value)
        currentItems.value = emptyList()
        persistItems()
    }

    // ---- Undo / Redo -------------------------------------------

    fun undo() {
        val undoStack = if (_mode.value == PromptMode.POSITIVE) _positiveUndoStack else _negativeUndoStack
        val redoStack = if (_mode.value == PromptMode.POSITIVE) _positiveRedoStack else _negativeRedoStack
        val prev = undoStack.value.lastOrNull() ?: return
        redoStack.update { it + listOf(currentItems.value) }
        undoStack.update { it.dropLast(1) }
        currentItems.value = prev
        persistItems()
    }

    fun redo() {
        val undoStack = if (_mode.value == PromptMode.POSITIVE) _positiveUndoStack else _negativeUndoStack
        val redoStack = if (_mode.value == PromptMode.POSITIVE) _positiveRedoStack else _negativeRedoStack
        val next = redoStack.value.lastOrNull() ?: return
        undoStack.update { stack ->
            val pushed = stack + listOf(currentItems.value)
            if (pushed.size > HISTORY_LIMIT) pushed.drop(1) else pushed
        }
        redoStack.update { it.dropLast(1) }
        currentItems.value = next
        persistItems()
    }

    // ---- テンプレート ------------------------------------------

    fun addTemplateItems(templateText: String) {
        val words = templateText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (words.isEmpty()) return
        pushHistory(_positiveItems.value)
        val baseId = -System.currentTimeMillis()
        _positiveItems.update { current ->
            val existing = current.map { it.wordEn }.toSet()
            val newItems = words.filterNot { it in existing }
                .mapIndexed { i, w -> PromptItem(wordId = baseId - i, wordEn = w, wordJa = "") }
            current + newItems
        }
        persistItems()
    }

    // ---- 保存済みプロンプトから読み込み ----------------------------------------

    fun loadFromSaved(text: String, targetMode: PromptMode) {
        val words = text.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (words.isEmpty()) return
        val targetItems = if (targetMode == PromptMode.POSITIVE) _positiveItems else _negativeItems
        pushHistoryFor(targetMode)
        val baseId = -System.currentTimeMillis()
        targetItems.update { current ->
            val existing = current.map { it.wordEn }.toSet()
            val newItems = words.filterNot { it in existing }
                .mapIndexed { i, w -> PromptItem(wordId = baseId - i, wordEn = w, wordJa = "") }
            current + newItems
        }
        persistItems()
    }

    private fun pushHistoryFor(mode: PromptMode) {
        val targetItems = if (mode == PromptMode.POSITIVE) _positiveItems else _negativeItems
        val undoStack = if (mode == PromptMode.POSITIVE) _positiveUndoStack else _negativeUndoStack
        val redoStack = if (mode == PromptMode.POSITIVE) _positiveRedoStack else _negativeRedoStack
        undoStack.update { stack ->
            val next = stack + listOf(targetItems.value)
            if (next.size > HISTORY_LIMIT) next.drop(1) else next
        }
        redoStack.value = emptyList()
    }

    // ---- クリップボードインポート ----------------------------------------

    /**
     * ボトムシートで確定したアイテムをエリアAに追加し、
     * 必要に応じて未分類カテゴリにも登録する。
     *
     * [items] はプレビューシートで編集済みのリスト。
     * isEnabled = false のアイテムはスキップする。
     * registerToDb = true のアイテムのみ未分類カテゴリに登録する。
     *
     * DB登録はポジ・ネガの現在モードで分岐する。
     * isNegative = true のとき UNCATEGORIZED_NEGATIVE に登録される。
     */
    fun confirmClipboardImport(items: List<ClipboardImportItem>) {
        val enabledItems = items.filter { it.isEnabled }
        if (enabledItems.isEmpty()) return

        val isNegative = _mode.value == PromptMode.NEGATIVE
        pushHistory(currentItems.value)
        val baseId = -System.currentTimeMillis()

        currentItems.update { current ->
            val existing = current.map { it.wordEn }.toSet()
            val newItems = enabledItems
                .filterNot { it.wordEn in existing }
                .mapIndexed { i, item ->
                    PromptItem(wordId = baseId - i, wordEn = item.wordEn, wordJa = "")
                }
            current + newItems
        }
        persistItems()

        // DB登録対象があれば未分類カテゴリに登録
        val wordsToRegister = enabledItems
            .filter { it.registerToDb }
            .joinToString(",") { it.wordEn }

        if (wordsToRegister.isNotBlank()) {
            viewModelScope.launch {
                repository.registerNewWordsFromText(wordsToRegister, isNegative)
            }
        }
    }

    // ---- プロンプト生成 ----------------------------------------

    fun buildPromptText(): String =
        buildTextFromItems(
            if (_mode.value == PromptMode.POSITIVE) _positiveItems.value else _negativeItems.value
        )

    private fun buildTextFromItems(items: List<PromptItem>): String =
        items.mapIndexed { i, item -> if (i == 0) item.formatted else ", ${item.formatted}" }
            .joinToString("")

    // ---- 保存 --------------------------------------------------

    fun saveCurrentPrompt(title: String) {
        val posText = buildTextFromItems(_positiveItems.value)
        val negText = buildTextFromItems(_negativeItems.value)
        if (posText.isBlank() && negText.isBlank()) return
        viewModelScope.launch {
            repository.savePrompt(
                SavedPromptEntity(
                    title        = title,
                    promptText   = posText,
                    negativeText = negText,
                )
            )
        }
    }
}

// ---- 変換ヘルパー ----------------------------------------

private fun PromptItem.toPersistedItem() = PersistedPromptItem(
    wordId = wordId,
    wordEn = wordEn,
    wordJa = wordJa,
    weight = weight,
)

private fun PersistedPromptItem.toPromptItem() = PromptItem(
    wordId = wordId,
    wordEn = wordEn,
    wordJa = wordJa,
    weight = weight,
)