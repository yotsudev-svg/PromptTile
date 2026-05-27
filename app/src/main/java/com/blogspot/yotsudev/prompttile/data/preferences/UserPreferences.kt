package com.blogspot.yotsudev.prompttile.data.preferences


enum class ThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}

enum class ManagementFilterMode {
    ALL, ENABLED_ONLY, DISABLED_ONLY
}

data class UserPreferences(
    val themeConfig: ThemeConfig = ThemeConfig.FOLLOW_SYSTEM,
    val managementFilterMode: ManagementFilterMode = ManagementFilterMode.ALL,
    val moveToBackOnCopy: Boolean = false,

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
    val toppingGroupId: Long? = null,
    val selectedTopping: String? = null,
)
