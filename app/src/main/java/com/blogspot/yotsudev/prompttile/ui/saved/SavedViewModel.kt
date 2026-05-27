package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.preferences.ManagementFilterMode
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.UserPreferences
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: PromptRepository,
    private val dataSource: PreferencesDataSource,
) : ViewModel() {

    private val _prefs = dataSource.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(),
    )

    val filterMode = combine(_prefs) { it[0].managementFilterMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManagementFilterMode.ALL
    )

    val savedPrompts = combine(
        repository.savedPrompts,
        filterMode
    ) { prompts, mode ->
        when (mode) {
            ManagementFilterMode.ALL -> prompts.sortedBy { !it.isEnabled }
            ManagementFilterMode.ENABLED_ONLY -> prompts.filter { it.isEnabled }
            ManagementFilterMode.DISABLED_ONLY -> prompts.filter { !it.isEnabled }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun setFilterMode(mode: ManagementFilterMode) {
        viewModelScope.launch { dataSource.updateManagementFilterMode(mode) }
    }

    fun delete(entity: SavedPromptEntity) {
        if (entity.isDefault) return
        viewModelScope.launch { repository.deletePrompt(entity) }
    }

    fun updatePrompt(entity: SavedPromptEntity) {
        viewModelScope.launch { repository.updatePrompt(entity) }
    }

    fun togglePromptEnabled(entity: SavedPromptEntity) {
        viewModelScope.launch {
            repository.updatePrompt(entity.copy(isEnabled = !entity.isEnabled))
        }
    }

    fun addManualPrompt(title: String, positiveText: String, negativeText: String) {
        val pos = positiveText.trim()
        val neg = negativeText.trim()
        if (pos.isBlank() && neg.isBlank()) return

        val resolvedTitle = title.trim().ifBlank {
            java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
        }

        viewModelScope.launch {
            coroutineScope {
                launch {
                    repository.savePrompt(
                        SavedPromptEntity(
                            title        = resolvedTitle,
                            promptText   = pos,
                            negativeText = neg,
                        )
                    )
                }
                if (pos.isNotBlank()) {
                    launch { repository.registerNewWordsFromText(pos, isNegative = false) }
                }
                if (neg.isNotBlank()) {
                    launch { repository.registerNewWordsFromText(neg, isNegative = true) }
                }
            }
        }
    }
}
