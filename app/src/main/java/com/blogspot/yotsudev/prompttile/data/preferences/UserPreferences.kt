package com.blogspot.yotsudev.prompttile.data.preferences


enum class ThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}
data class UserPreferences(
    val themeConfig: ThemeConfig = ThemeConfig.FOLLOW_SYSTEM,
    val moveToBackOnCopy: Boolean = false,
    val userTemplates: List<Pair<String, String>> = emptyList(),

    /**
     * アプリ終了時に保持するポジティブ・ネガティブのアイテムリスト。
     * DataStore は文字列のみ保存できるため、
     * "wordId::wordEn::wordJa::weight|..." 形式にエンコードして保存する。
     * weight が null の場合は "null" という文字列として保存する。
     */
    val persistedPositiveItems: List<PersistedPromptItem> = emptyList(),
    val persistedNegativeItems: List<PersistedPromptItem> = emptyList(),

    /**
     * 最近使った単語のIDリスト。
     */
    val recentWordIds: List<Long> = emptyList(),
)

/**
 * DataStore に保存するための PromptItem の軽量版。
 * PromptItem は UI層のクラスなので、データ層では専用の型を使う。
 * wordId は復元時に負の値（-時刻ベース）を再生成するため保存しない。
 */
data class PersistedPromptItem(
    val wordId: Long,   // 復元時に選択状態を正しく判定するために保存する
    val wordEn: String,
    val wordJa: String,
    val weight: Float?,
)