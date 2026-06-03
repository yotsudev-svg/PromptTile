package com.blogspot.yotsudev.prompttile.ui.import_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.importer.ImportCategory
import com.blogspot.yotsudev.prompttile.data.importer.ImportParseResult
import com.blogspot.yotsudev.prompttile.data.importer.JsonImportParser
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- UI State ----------------------------------------------------------

data class ImportUiState(
    val jsonInput: String = "",
    val parseError: String? = null,
    val preview: List<ImportCategory> = emptyList(),
    val isImporting: Boolean = false,
    val importDone: Boolean = false,
    val importSummary: String = "",  // 「3カテゴリ、25単語を追加しました」
)

// ---- ViewModel ---------------------------------------------------------

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: PromptRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    /** テキストフィールドの入力を受け取り、即座にパースしてプレビューを更新する */
    fun onJsonInput(text: String) {
        _uiState.update { it.copy(jsonInput = text, parseError = null, preview = emptyList()) }
        if (text.isBlank()) return

        when (val result = JsonImportParser.parse(text)) {
            is ImportParseResult.Success -> {
                _uiState.update { it.copy(preview = result.categories) }
            }
            is ImportParseResult.Failure -> {
                _uiState.update { it.copy(parseError = result.message) }
            }
        }
    }

    /** 「インポート実行」ボタン押下時 */
    fun executeImport() {
        val categories = _uiState.value.preview
        if (categories.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }

            var addedCategories = 0
            var addedWords = 0

            categories.forEach { importCat ->
                // 同名カテゴリが既にあれば再利用、なければ新規作成
                val existing = repository.getCategoryByNameEn(importCat.nameEn)
                val categoryId = if (existing != null) {
                    existing.id
                } else {
                    val maxOrder = repository.getMaxCategorySortOrder()
                    val newCat = CategoryEntity(
                        nameJa     = importCat.nameJa,
                        nameEn     = importCat.nameEn,
                        parentId   = importCat.parentId,
                        isNegative = importCat.isNegative,
                        sortOrder  = maxOrder + 1,
                    )
                    addedCategories++
                    repository.insertCategory(newCat)
                }

                // 既存単語と重複しないものだけ挿入
                val existingWords = repository.getWordEnsByCategory(categoryId).toHashSet()
                val newWords = importCat.words
                    .filter { it.wordEn !in existingWords }
                    .mapIndexed { i, w ->
                        PromptWordEntity(
                            categoryId = categoryId,
                            wordEn     = w.wordEn,
                            wordJa     = w.wordJa,
                            sortOrder  = i,
                        )
                    }

                if (newWords.isNotEmpty()) {
                    repository.insertWords(newWords)
                    addedWords += newWords.size
                }
            }

            val summary = buildString {
                if (addedCategories > 0) append("${addedCategories}カテゴリ、")
                append("${addedWords}単語を追加しました")
                if (addedCategories == 0) append("（既存カテゴリに追記）")
            }

            _uiState.update {
                it.copy(
                    isImporting   = false,
                    importDone    = true,
                    importSummary = summary,
                )
            }
        }
    }

    /** 画面を初期状態に戻す（別のJSONを試したい時） */
    fun reset() {
        _uiState.value = ImportUiState()
    }
}