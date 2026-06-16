package com.blogspot.yotsudev.prompttile.ui.import_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

            val (addedCategories, addedWords) = repository.importCategories(categories)

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
