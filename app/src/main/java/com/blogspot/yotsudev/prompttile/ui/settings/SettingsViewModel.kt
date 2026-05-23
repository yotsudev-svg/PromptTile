package com.blogspot.yotsudev.prompttile.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
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
        if (prefs == null) return@combine emptyList()

        val mappedDefaults = defaults.map { d ->
            d.copy(isEnabled = !prefs.disabledDefaultTemplateNames.contains(d.name))
        }
        val userTemplates = prefs.userTemplates.map { ut ->
            PrefixTemplate(name = ut.name, text = ut.text, isDefault = false, isEnabled = ut.isEnabled)
        }

        mappedDefaults + userTemplates
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            val loadTemplatesJob = launch { loadDefaultTemplates() }
            preferences.filterNotNull().first()
            loadTemplatesJob.join()
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

    fun removeUserTemplate(template: PrefixTemplate) {
        viewModelScope.launch { dataSource.removeUserTemplate(template.name, template.text) }
    }

    fun toggleTemplateEnabled(template: PrefixTemplate) {
        viewModelScope.launch {
            if (template.isDefault) {
                dataSource.toggleDefaultTemplateEnabled(template.name)
            } else {
                dataSource.toggleUserTemplateEnabled(template.name, template.text)
            }
        }
    }
}
