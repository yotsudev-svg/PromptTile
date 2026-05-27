package com.blogspot.yotsudev.prompttile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSource: PreferencesDataSource,
) : ViewModel() {

    val preferences = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun updateThemeConfig(config: ThemeConfig) {
        viewModelScope.launch { dataSource.updateThemeConfig(config) }
    }

    fun updateMoveToBack(enabled: Boolean) {
        viewModelScope.launch { dataSource.updateMoveToBack(enabled) }
    }
}
