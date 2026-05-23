package com.blogspot.yotsudev.prompttile.data.preferences


enum class ThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}

data class UserPreferences(
    val themeConfig: ThemeConfig = ThemeConfig.FOLLOW_SYSTEM,
    val moveToBackOnCopy: Boolean = false,
    val userTemplates: List<UserTemplate> = emptyList(),
    val disabledDefaultTemplateNames: Set<String> = emptySet(),

    /**
     * アプリ終了時に保持するポジティブ・ネガティブのアイテムリスト。
     */
    val persistedPositiveItems: List<PersistedPromptItem> = emptyList(),
    val persistedNegativeItems: List<PersistedPromptItem> = emptyList(),
)

/**
 * DataStore に保存するための PromptItem の軽量版。
 */
data class PersistedPromptItem(
    val wordId: Long,   // 復元時に選択状態を正しく判定するために保存する
    val wordEn: String,
    val wordJa: String,
    val weight: Float?,
)

data class UserTemplate(
    val name: String,
    val text: String,
    val isEnabled: Boolean = true
)
