package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: PromptRepository,
) : ViewModel() {

    val savedPrompts = repository.savedPrompts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun delete(entity: SavedPromptEntity) {
        viewModelScope.launch { repository.deletePrompt(entity) }
    }

    /**
     * 手動入力したプロンプトを保存し、未登録単語を自動登録する。
     *
     * savePrompt と registerNewWordsFromText を並列で実行せず
     * 順番に実行している理由:
     * どちらも独立したDB操作なので並列でも問題ないが、
     * シンプルさを優先して直列にしている。
     * 単語数が多い場合でも数十ms程度なのでUXへの影響はない。
     */
    fun addManualPrompt(title: String, positiveText: String, negativeText: String) {
        val pos = positiveText.trim()
        val neg = negativeText.trim()
        if (pos.isBlank() && neg.isBlank()) return

        val resolvedTitle = title.trim().ifBlank {
            java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
        }

        viewModelScope.launch {
            // 1. プロンプトを保存
            repository.savePrompt(
                SavedPromptEntity(
                    title        = resolvedTitle,
                    promptText   = pos,
                    negativeText = neg,
                )
            )
            // 2. 未登録単語を未分類カテゴリに自動登録
            if (pos.isNotBlank()) {
                repository.registerNewWordsFromText(pos, isNegative = false)
            }
            if (neg.isNotBlank()) {
                repository.registerNewWordsFromText(neg, isNegative = true)
            }
        }
    }
}