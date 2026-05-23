package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
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
     * savePrompt と registerNewWordsFromText は互いに依存しないため
     * coroutineScope 内で並列実行する。
     *
     * 直列実行との違い:
     *   直列: savePrompt → registerPos → registerNeg（最大3往復）
     *   並列: savePrompt と register 系を同時に実行（1往復分の時間に短縮）
     *
     * coroutineScope を使う理由:
     *   launch { launch{} launch{} } ではなく coroutineScope { launch{} launch{} } にすることで、
     *   内側の全 launch が完了するまで外側のコルーチンが終了しない（構造化並行性）。
     *   いずれかの処理が例外を投げた場合も全体がキャンセルされ、中途半端な状態にならない。
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
            coroutineScope {
                // savePrompt と単語登録を並列で実行
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