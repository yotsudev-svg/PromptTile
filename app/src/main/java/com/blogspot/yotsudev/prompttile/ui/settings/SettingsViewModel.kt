package com.blogspot.yotsudev.prompttile.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import com.blogspot.yotsudev.prompttile.data.preferences.UserPreferences
import com.blogspot.yotsudev.prompttile.data.seed.PrefixTemplate
import com.blogspot.yotsudev.prompttile.data.seed.parsePrefixTemplates
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSource: PreferencesDataSource,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // 1. スプラッシュ画面を終了して良いかを判定するフラグ
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    val preferences = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _defaultTemplates = MutableStateFlow<List<PrefixTemplate>>(emptyList())

    val allTemplates = combine(
        _defaultTemplates,
        preferences,
    ) { defaults, prefs ->
        val userTemplates = prefs?.userTemplates?.map { (name, text) ->
            PrefixTemplate(name = name, text = text, isDefault = false)
        } ?: emptyList()

        defaults + userTemplates
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        // 全ての初期化処理を並列で実行し、完了を待つ
        viewModelScope.launch {
            // A. デフォルトテンプレートのロード
            val loadTemplatesJob = launch { loadDefaultTemplates() }

            // B. DataStore（preferences）が non-null になるまで待機
            preferences.filterNotNull().first()

            // 両方が終わるのを待つ（Aは内部で処理が終わるのを待つ）
            loadTemplatesJob.join()

            // 2. 全ての準備が整ったらフラグを true にする
            _isReady.value = true
        }
    }

    private suspend fun loadDefaultTemplates() = withContext(Dispatchers.IO) {
        val json = context.assets
            .open("seed_data.json")
            .bufferedReader()
            .use { it.readText() }
        _defaultTemplates.value = parsePrefixTemplates(json)
    }

    fun updateThemeConfig(config: ThemeConfig) {
        viewModelScope.launch { dataSource.updateThemeConfig(config) }
    }

    fun updateMoveToBack(enabled: Boolean) {
        viewModelScope.launch { dataSource.updateMoveToBack(enabled) }
    }

    fun addUserTemplate(name: String, text: String) {
        viewModelScope.launch { dataSource.addUserTemplate(name, text) }
    }

    fun removeUserTemplate(name: String, text: String) {
        viewModelScope.launch { dataSource.removeUserTemplate(name, text) }
    }
}