package com.blogspot.yotsudev.prompttile.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.importer.ImportParseResult
import com.blogspot.yotsudev.prompttile.data.importer.JsonExporter
import com.blogspot.yotsudev.prompttile.data.importer.JsonImportParser
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSource: PreferencesDataSource,
    private val repository: PromptRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val preferences = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun updateThemeConfig(config: ThemeConfig) {
        viewModelScope.launch { dataSource.updateThemeConfig(config) }
    }

    fun updateMoveToBack(enabled: Boolean) {
        viewModelScope.launch { dataSource.updateMoveToBack(enabled) }
    }

    fun updateGridColumnsConfig(config: com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig) {
        viewModelScope.launch { dataSource.updateGridColumnsConfig(config) }
    }

    fun updateMaxHistoryCount(count: Int) {
        viewModelScope.launch { dataSource.updateMaxHistoryCount(count) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearAllHistory() }
    }

    fun consumeMessage() {
        _message.value = ""
    }

    /** ファイルからデータを読み込み、DBにマージする（復元） */
    fun loadJsonFromUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    }
                }.getOrNull()
            }

            if (text == null) {
                _message.value = "ファイルの読み込みに失敗しました"
                _isProcessing.value = false
                return@launch
            }

            when (val result = JsonImportParser.parse(text)) {
                is ImportParseResult.Success -> {
                    val (catCount, wordCount) = repository.importCategories(result.categories)
                    _message.value = "${catCount}カテゴリ、${wordCount}単語を復元しました"
                }
                is ImportParseResult.Failure -> {
                    _message.value = "解析失敗: ${result.message}"
                }
            }
            _isProcessing.value = false
        }
    }

    /** 現在のDB内容をJSONファイルとして書き出す（バックアップ） */
    fun exportToJsonUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            runCatching {
                val categories = repository.allCategories.first()
                val allWords = repository.allWordsWithCategory.first().map { it.word }
                val jsonString = JsonExporter.export(categories, allWords)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.bufferedWriter().use { it.write(jsonString) }
                    }
                }
            }.onSuccess {
                _message.value = "バックアップが完了しました"
            }.onFailure { e ->
                _message.value = "エクスポート失敗: ${e.message}"
            }
            _isProcessing.value = false
        }
    }
}
