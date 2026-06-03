package com.blogspot.yotsudev.prompttile.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity
import com.blogspot.yotsudev.prompttile.data.preferences.PersistedPromptItem
import com.blogspot.yotsudev.prompttile.data.preferences.PersistedSelectedTopping
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.UserPreferences
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import com.blogspot.yotsudev.prompttile.data.seed.PrefixTemplate
import com.blogspot.yotsudev.prompttile.data.seed.parsePrefixTemplates
import com.blogspot.yotsudev.prompttile.util.PromptFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val HISTORY_LIMIT = 10

@HiltViewModel
class PromptViewModel @Inject constructor(
    private val repository: PromptRepository,
    private val dataSource: PreferencesDataSource,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _mode = MutableStateFlow(PromptMode.POSITIVE)
    private val _positiveItems = MutableStateFlow<List<PromptItem>>(emptyList())
    private val _negativeItems = MutableStateFlow<List<PromptItem>>(emptyList())

    private val _selectedPositiveParentId = MutableStateFlow<Long?>(null)
    private val _selectedNegativeParentId = MutableStateFlow<Long?>(null)
    private val _selectedPositiveCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedNegativeCategoryId = MutableStateFlow<Long?>(null)

    private val _searchQuery = MutableStateFlow("")
    private val _defaultTemplates = MutableStateFlow<List<PrefixTemplate>>(emptyList())

    private val _positiveUndoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())
    private val _positiveRedoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())
    private val _negativeUndoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())
    private val _negativeRedoStack = MutableStateFlow<List<List<PromptItem>>>(emptyList())

    // ---- マルチ調整シート用 ----
    /** 現在シートで編集中のアイテム（null = シート非表示） */
    private val _adjustingItem = MutableStateFlow<PromptItem?>(null)
    /** 編集中アイテムのトッピンググループと選択肢（DB から非同期取得） */
    private val _adjustingToppingGroups = MutableStateFlow<List<ToppingGroupWithItems>>(emptyList())

    private val _prefs = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _parentCategories = _mode.flatMapLatest { mode ->
        repository.observeParentCategories(isNegative = mode == PromptMode.NEGATIVE)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _resolvedSelectedParentId = combine(
        _mode,
        _selectedPositiveParentId,
        _selectedNegativeParentId,
        _parentCategories,
    ) { mode, posId, negId, parents ->
        val currentId = if (mode == PromptMode.POSITIVE) posId else negId
        currentId ?: parents.firstOrNull()?.id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _childCategories = combine(
        _mode,
        _resolvedSelectedParentId
    ) { mode, parentId ->
        mode to parentId
    }.flatMapLatest { (mode, parentId) ->
        if (parentId == null) flowOf(emptyList())
        else repository.observeCategoriesByParent(parentId, isNegative = mode == PromptMode.NEGATIVE)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _resolvedSelectedCategoryId = combine(
        _mode,
        _selectedPositiveCategoryId,
        _selectedNegativeCategoryId,
        _childCategories
    ) { mode, posId, negId, children ->
        if (mode == PromptMode.POSITIVE) posId ?: children.firstOrNull()?.id
        else negId ?: children.firstOrNull()?.id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _wordsInCategory = _resolvedSelectedCategoryId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.visibleWordsByCategory(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _searchResults = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else repository.searchWords(q)
    }

    private data class HistoryState(val canUndo: Boolean, val canRedo: Boolean)

    private val _historyState = combine(
        _mode, _positiveUndoStack, _positiveRedoStack, _negativeUndoStack, _negativeRedoStack,
    ) { mode, posUndo, posRedo, negUndo, negRedo ->
        HistoryState(
            canUndo = if (mode == PromptMode.POSITIVE) posUndo.isNotEmpty() else negUndo.isNotEmpty(),
            canRedo = if (mode == PromptMode.POSITIVE) posRedo.isNotEmpty() else negRedo.isNotEmpty(),
        )
    }

    private val _allTemplates = repository.savedPrompts.map { list ->
        list.filter { it.isEnabled }
    }

    val uiState = combine(
        _mode, _positiveItems, _negativeItems,
        _parentCategories,
        _resolvedSelectedParentId, _childCategories,
        _resolvedSelectedCategoryId, _wordsInCategory, _searchResults,
        _prefs, _historyState, _searchQuery, _allTemplates,
        _adjustingItem, _adjustingToppingGroups,
    ) { args ->
        val mode           = args[0] as PromptMode
        val posItems       = args[1] as List<PromptItem>
        val negItems       = args[2] as List<PromptItem>
        val parents        = args[3] as List<com.blogspot.yotsudev.prompttile.data.entity.ParentCategoryEntity>
        val resolvedParId  = args[4] as Long?
        val childCats      = args[5] as List<CategoryEntity>
        val resolvedCatId  = args[6] as Long?
        val words          = args[7] as List<PromptWordEntity>
        val searchResults  = args[8] as List<PromptWordEntity>
        val prefs          = args[9] as UserPreferences
        val history        = args[10] as HistoryState
        val query          = args[11] as String
        val templates      = args[12] as List<SavedPromptEntity>
        val adjItem        = args[13] as PromptItem?
        val adjGroups      = args[14] as List<ToppingGroupWithItems>

        PromptUiState(
            mode                       = mode,
            positiveItems              = posItems,
            positiveParentCategories   = if (mode == PromptMode.POSITIVE) parents else emptyList(),
            selectedPositiveParentId   = if (mode == PromptMode.POSITIVE) resolvedParId else _selectedPositiveParentId.value,
            positiveCategories         = if (mode == PromptMode.POSITIVE) childCats else emptyList(),
            selectedPositiveCategoryId = if (mode == PromptMode.POSITIVE) resolvedCatId else _selectedPositiveCategoryId.value,
            
            negativeItems              = negItems,
            negativeParentCategories   = if (mode == PromptMode.NEGATIVE) parents else emptyList(),
            selectedNegativeParentId   = if (mode == PromptMode.NEGATIVE) resolvedParId else _selectedNegativeParentId.value,
            negativeCategories         = if (mode == PromptMode.NEGATIVE) childCats else emptyList(),
            selectedNegativeCategoryId = if (mode == PromptMode.NEGATIVE) resolvedCatId else _selectedNegativeCategoryId.value,

            wordsInCategory            = words,
            isLoading                  = false,
            moveToBackOnCopy           = prefs.moveToBackOnCopy,
            allTemplates               = templates,
            canUndo                    = history.canUndo,
            canRedo                    = history.canRedo,
            positivePromptText         = PromptFormatter.formatPrompt(posItems),
            negativePromptText         = PromptFormatter.formatPrompt(negItems),
            searchQuery                = query,
            searchResults              = searchResults,
            adjustingItem              = adjItem,
            adjustingToppingGroups     = adjGroups,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PromptUiState(),
    )

    // ---- 起動時の状態復元 ----------------------------------------

    init {
        viewModelScope.launch {
            launch { loadDefaultTemplates() }
            val prefs = dataSource.userPreferences.first()
            if (prefs.persistedPositiveItems.isNotEmpty())
                _positiveItems.value = prefs.persistedPositiveItems.map { it.toPromptItem() }
            if (prefs.persistedNegativeItems.isNotEmpty())
                _negativeItems.value = prefs.persistedNegativeItems.map { it.toPromptItem() }
        }
    }

    private suspend fun loadDefaultTemplates() = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open("seed_data.json").bufferedReader().use { it.readText() }
            _defaultTemplates.value = parsePrefixTemplates(json)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ---- 自動保存 ----------------------------------------

    private fun persistItems() {
        viewModelScope.launch {
            dataSource.updatePersistedItems(
                positiveItems = _positiveItems.value.map { it.toPersistedItem() },
                negativeItems = _negativeItems.value.map { it.toPersistedItem() },
            )
        }
    }

    // ---- 履歴ヘルパー ----------------------------------------

    private fun pushHistory(current: List<PromptItem>) {
        val undoStack = if (_mode.value == PromptMode.POSITIVE) _positiveUndoStack else _negativeUndoStack
        val redoStack = if (_mode.value == PromptMode.POSITIVE) _positiveRedoStack else _negativeRedoStack
        undoStack.update { stack ->
            (stack + listOf(current)).let { if (it.size > HISTORY_LIMIT) it.drop(1) else it }
        }
        redoStack.value = emptyList()
    }

    private fun getHistoryStacks() =
        if (_mode.value == PromptMode.POSITIVE) _positiveUndoStack to _positiveRedoStack
        else _negativeUndoStack to _negativeRedoStack

    private val currentItems
        get() = if (_mode.value == PromptMode.POSITIVE) _positiveItems else _negativeItems

    // ---- モード切り替え ----------------------------------------

    fun switchMode(mode: PromptMode) {
        _mode.value = mode
        // モード切り替え時に選択状態をリセットし、不整合によるクラッシュを防ぐ
        _selectedPositiveParentId.value = null
        _selectedNegativeParentId.value = null
        _selectedPositiveCategoryId.value = null
        _selectedNegativeCategoryId.value = null
    }

    // ---- カテゴリ選択 ----------------------------------------

    fun selectParent(id: Long) {
        if (_mode.value == PromptMode.POSITIVE) {
            _selectedPositiveParentId.value = id
            _selectedPositiveCategoryId.value = null // reset child to trigger auto-select
        } else {
            _selectedNegativeParentId.value = id
            _selectedNegativeCategoryId.value = null
        }
    }

    fun selectCategory(id: Long) {
        if (_mode.value == PromptMode.POSITIVE) _selectedPositiveCategoryId.value = id
        else _selectedNegativeCategoryId.value = id
    }

    // ---- 単語操作 ----------------------------------------

    /**
     * WordPool の通常タップ（左エリア）。
     * トッピング情報を PromptItem に引き継ぐため toppingGroupIds を渡す。
     */
    fun toggleWord(word: PromptWordEntity) {
        pushHistory(currentItems.value)
        currentItems.update { current ->
            if (current.any { it.wordId == word.id }) {
                current.filter { it.wordId != word.id }
            } else {
                current + PromptItem(
                    wordId          = word.id,
                    wordEn          = word.wordEn,
                    wordJa          = word.wordJa,
                    toppingGroupIds = word.toppingGroupIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList(),
                    excludeToppingValues = word.excludeToppingValues?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                )
            }
        }
        persistItems()
    }

    /**
     * WordPool の分割チップ右エリア（🎨）タップ。
     * トッピング選択シートを経由して単語を追加する。
     * selectedTopping を指定して直接 PromptItem を追加する。
     */
    fun addWordWithTopping(word: PromptWordEntity, groupId: Long, topping: String?, isPrefix: Boolean) {
        viewModelScope.launch {
            val priority = calculatePriority(groupId, isPrefix)
            pushHistory(currentItems.value)
            currentItems.update { current ->
                val existing = current.firstOrNull { it.wordId == word.id }
                val toppingIds = word.toppingGroupIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
                val excludeValues = word.excludeToppingValues?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

                if (existing != null) {
                    current.map {
                        if (it.wordId == word.id) {
                            val newToppings = it.selectedToppings.filterNot { t -> t.groupId == groupId }.toMutableList()
                            if (topping != null) {
                                newToppings.add(SelectedTopping(groupId, topping, isPrefix, priority))
                            }
                            it.copy(selectedToppings = newToppings)
                        } else it
                    }
                } else {
                    val selected = if (topping != null) listOf(SelectedTopping(groupId, topping, isPrefix, priority)) else emptyList()
                    current + PromptItem(
                        wordId          = word.id,
                        wordEn          = word.wordEn,
                        wordJa          = word.wordJa,
                        toppingGroupIds = toppingIds,
                        selectedToppings = selected,
                        excludeToppingValues = excludeValues,
                    )
                }
            }
            persistItems()
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun removeItem(item: PromptItem) {
        pushHistory(currentItems.value)
        currentItems.update { it.filter { i -> i.wordId != item.wordId } }
        persistItems()
    }

    fun moveItem(from: Int, to: Int) {
        pushHistory(currentItems.value)
        currentItems.update { current ->
            current.toMutableList().apply { add(to, removeAt(from)) }
        }
        persistItems()
    }

    fun clearAll() {
        pushHistory(currentItems.value)
        currentItems.update { emptyList() }
        persistItems()
    }

    // ---- Undo / Redo ----------------------------------------

    fun undo() {
        val (undoStack, redoStack) = getHistoryStacks()
        val prev = undoStack.value.lastOrNull() ?: return
        redoStack.update { it + listOf(currentItems.value) }
        undoStack.update { it.dropLast(1) }
        currentItems.value = prev
        persistItems()
    }

    fun redo() {
        val (undoStack, redoStack) = getHistoryStacks()
        val next = redoStack.value.lastOrNull() ?: return
        undoStack.update { stack ->
            (stack + listOf(currentItems.value)).let { if (it.size > HISTORY_LIMIT) it.drop(1) else it }
        }
        redoStack.update { it.dropLast(1) }
        currentItems.value = next
        persistItems()
    }

    // ---- マルチ調整ボトムシート ----------------------------------------

    /**
     * プロンプトエリアのチップをタップしたとき呼び出す。
     * 対象アイテムをシートにセットし、トッピング選択肢を非同期で取得する。
     */
    fun openAdjustSheet(item: PromptItem) {
        _adjustingItem.value = item
        _adjustingToppingGroups.value = emptyList()
        if (item.toppingGroupIds.isNotEmpty()) {
            viewModelScope.launch {
                val groups = item.toppingGroupIds.mapNotNull { gid ->
                    val group = repository.getToppingGroup(gid) ?: return@mapNotNull null
                    val items = repository.getToppingItems(gid)
                    ToppingGroupWithItems(group, items)
                }
                _adjustingToppingGroups.value = groups
            }
        }
    }

    /** シートを閉じる */
    fun closeAdjustSheet() {
        _adjustingItem.value = null
        _adjustingToppingGroups.value = emptyList()
    }

    suspend fun getToppingItems(groupId: Long) = repository.getToppingItems(groupId)

    suspend fun getToppingGroup(groupId: Long) = repository.getToppingGroup(groupId)

    /**
     * シート内で重みを変更する。
     * null = なし（1.0扱い）、それ以外は数値をそのまま設定。
     */
    fun setWeight(item: PromptItem, weight: Float?) {
        currentItems.update { current ->
            current.map { if (it.wordId == item.wordId) it.copy(weight = weight) else it }
        }
        // _adjustingItem も同期して更新（シートのUI反映のため）
        _adjustingItem.update { it?.takeIf { a -> a.wordId == item.wordId }?.copy(weight = weight) ?: it }
        persistItems()
    }

    /**
     * シート内でトッピングを変更する。
     * null を渡すとトッピングなし（wordEn のみ）に戻る。
     */
    fun setTopping(item: PromptItem, groupId: Long, topping: String?, isPrefix: Boolean) {
        viewModelScope.launch {
            val priority = calculatePriority(groupId, isPrefix)
            currentItems.update { current ->
                current.map {
                    if (it.wordId == item.wordId) {
                        val newToppings = it.selectedToppings.filterNot { t -> t.groupId == groupId }.toMutableList()
                        if (topping != null) {
                            newToppings.add(SelectedTopping(groupId, topping, isPrefix, priority))
                        }
                        it.copy(selectedToppings = newToppings)
                    } else it
                }
            }
            _adjustingItem.update {
                if (it?.wordId == item.wordId) {
                    val newToppings = it.selectedToppings.filterNot { t -> t.groupId == groupId }.toMutableList()
                    if (topping != null) {
                        newToppings.add(SelectedTopping(groupId, topping, isPrefix, priority))
                    }
                    it.copy(selectedToppings = newToppings)
                } else it
            }
            persistItems()
        }
    }

    private suspend fun calculatePriority(groupId: Long, isPrefix: Boolean): Int {
        if (!isPrefix) return 800 // Suffixes are always 800+

        val group = repository.getToppingGroup(groupId) ?: return 999
        val name = group.nameEn.lowercase()
        return when {
            name.contains("size") || name.contains("volume") || name.contains("length") -> 100
            name.contains("condition") || name.contains("state") || name.contains("status") || name.contains("appearance") -> 200
            name.contains("shape") || name.contains("cut") || name.contains("style") -> 300
            name.contains("color") -> 400
            name.contains("material") || name.contains("texture") -> 500
            name.contains("pattern") || name.contains("print") -> 600
            else -> 999
        }
    }

    // ---- テンプレート ----------------------------------------

    fun addTemplateItems(templateText: String) {
        val words = PromptFormatter.parsePromptText(templateText)
        if (words.isEmpty()) return
        pushHistory(currentItems.value)
        val baseId = -System.currentTimeMillis()
        currentItems.update { current ->
            val existing = current.map { it.wordEn }.toSet()
            val newItems = words.filterNot { it in existing }
                .mapIndexed { i, w -> PromptItem(wordId = baseId - i, wordEn = w, wordJa = "") }
            current + newItems
        }
        persistItems()
    }

    // ---- 保存済みプロンプト読み込み ----------------------------------------

    fun loadPromptSet(entity: SavedPromptEntity) {
        if (entity.promptText.isNotBlank()) loadFromSaved(entity.promptText, PromptMode.POSITIVE)
        if (entity.negativeText.isNotBlank()) loadFromSaved(entity.negativeText, PromptMode.NEGATIVE)
    }

    fun loadFromSaved(text: String, targetMode: PromptMode) {
        val words = PromptFormatter.parsePromptText(text)
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
        val undoStack   = if (mode == PromptMode.POSITIVE) _positiveUndoStack else _negativeUndoStack
        val redoStack   = if (mode == PromptMode.POSITIVE) _positiveRedoStack else _negativeRedoStack
        undoStack.update { stack ->
            (stack + listOf(targetItems.value)).let { if (it.size > HISTORY_LIMIT) it.drop(1) else it }
        }
        redoStack.value = emptyList()
    }

    // ---- クリップボードインポート ----------------------------------------

    fun confirmClipboardImport(items: List<ClipboardImportItem>) {
        val enabled = items.filter { it.isEnabled }
        if (enabled.isEmpty()) return
        val isNeg = _mode.value == PromptMode.NEGATIVE
        pushHistory(currentItems.value)
        val baseId = -System.currentTimeMillis()
        currentItems.update { current ->
            val existing = current.map { it.wordEn }.toSet()
            val new = enabled.filterNot { it.wordEn in existing }
                .mapIndexed { i, item -> PromptItem(wordId = baseId - i, wordEn = item.wordEn, wordJa = "") }
            current + new
        }
        persistItems()
        val toRegister = enabled.filter { it.registerToDb }.joinToString(",") { it.wordEn }
        if (toRegister.isNotBlank()) {
            viewModelScope.launch { repository.registerNewWordsFromText(toRegister, isNeg) }
        }
    }

    // ---- プロンプト生成 / 保存 ----------------------------------------

    fun buildPromptText(): String = PromptFormatter.formatPrompt(
        if (_mode.value == PromptMode.POSITIVE) _positiveItems.value else _negativeItems.value
    )

    fun saveCurrentPrompt(title: String) {
        val pos = PromptFormatter.formatPrompt(_positiveItems.value)
        val neg = PromptFormatter.formatPrompt(_negativeItems.value)
        if (pos.isBlank() && neg.isBlank()) return
        viewModelScope.launch {
            repository.savePrompt(SavedPromptEntity(title = title, promptText = pos, negativeText = neg))
        }
    }
}

// ---- 変換ヘルパー ----------------------------------------

private fun PromptItem.toPersistedItem() = PersistedPromptItem(
    wordId = wordId,
    wordEn = wordEn,
    wordJa = wordJa,
    weight = weight,
    toppingGroupIds = toppingGroupIds,
    selectedToppings = selectedToppings.map {
        PersistedSelectedTopping(it.groupId, it.valueEn, it.isPrefix, it.priority)
    },
    excludeToppingValues = excludeToppingValues,
)

private fun PersistedPromptItem.toPromptItem() = PromptItem(
    wordId = wordId,
    wordEn = wordEn,
    wordJa = wordJa,
    weight = weight,
    toppingGroupIds = toppingGroupIds,
    selectedToppings = selectedToppings.map {
        SelectedTopping(it.groupId, it.valueEn, it.isPrefix, it.priority)
    },
    excludeToppingValues = excludeToppingValues,
)
