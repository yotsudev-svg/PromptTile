package com.blogspot.yotsudev.prompttile.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_CONFIG = stringPreferencesKey("theme_config")
        val MOVE_TO_BACK = booleanPreferencesKey("move_to_back_on_copy")
        val USER_TEMPLATES = stringPreferencesKey("user_templates")
        val POSITIVE_ITEMS = stringPreferencesKey("positive_items")
        val NEGATIVE_ITEMS = stringPreferencesKey("negative_items")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->

        val themeString = prefs[Keys.THEME_CONFIG] ?: ThemeConfig.FOLLOW_SYSTEM.name
        val themeConfig = runCatching { ThemeConfig.valueOf(themeString) }.getOrDefault(ThemeConfig.FOLLOW_SYSTEM)

        UserPreferences(
            themeConfig = themeConfig,
            moveToBackOnCopy = prefs[Keys.MOVE_TO_BACK] ?: false,
            userTemplates = decodeTemplates(prefs[Keys.USER_TEMPLATES] ?: ""),
            persistedPositiveItems = decodeItems(prefs[Keys.POSITIVE_ITEMS] ?: ""),
            persistedNegativeItems = decodeItems(prefs[Keys.NEGATIVE_ITEMS] ?: ""),
        )
    }

    suspend fun updateThemeConfig(config: ThemeConfig) {
        context.dataStore.edit { it[Keys.THEME_CONFIG] = config.name }
    }

    suspend fun updateMoveToBack(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MOVE_TO_BACK] = enabled }
    }

    suspend fun addUserTemplate(name: String, text: String) {
        context.dataStore.edit { prefs ->
            val current = decodeTemplates(prefs[Keys.USER_TEMPLATES] ?: "").toMutableList()
            current.add(Pair(name, text))
            prefs[Keys.USER_TEMPLATES] = encodeTemplates(current)
        }
    }

    suspend fun removeUserTemplate(name: String, text: String) {
        context.dataStore.edit { prefs ->
            val current = decodeTemplates(prefs[Keys.USER_TEMPLATES] ?: "").toMutableList()
            current.removeAll { it.first == name && it.second == text }
            prefs[Keys.USER_TEMPLATES] = encodeTemplates(current)
        }
    }

    /**
     * ポジ・ネガのアイテムリストを DataStore に保存する。
     * 単語操作のたびに呼ばれるため、エンコードは軽量に保つ。
     */
    suspend fun updatePersistedItems(
        positiveItems: List<PersistedPromptItem>,
        negativeItems: List<PersistedPromptItem>,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POSITIVE_ITEMS] = encodeItems(positiveItems)
            prefs[Keys.NEGATIVE_ITEMS] = encodeItems(negativeItems)
        }
    }

    // ---- エンコード / デコード ----------------------------------------

    private fun encodeTemplates(list: List<Pair<String, String>>): String =
        list.joinToString("|") { "${it.first}::${it.second}" }

    private fun decodeTemplates(encoded: String): List<Pair<String, String>> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split("|").mapNotNull { entry ->
            val parts = entry.split("::")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        }
    }

    /**
     * PersistedPromptItem のリストを文字列にエンコードする。
     * フォーマット: "wordEn::wordJa::weight|wordEn::wordJa::weight|..."
     * weight が null のときは "null" という文字列で保存する。
     *
     * wordEn・wordJa に "::" や "|" が含まれる可能性は低いが、
     * 将来的な問題を避けるため区切り文字と衝突しない前提で設計している。
     * 衝突が懸念される場合は Base64 エンコードへの移行を検討すること。
     */
    private fun encodeItems(list: List<PersistedPromptItem>): String {
        if (list.isEmpty()) return ""
        return list.joinToString("|") { item ->
            "${item.wordId}::${item.wordEn}::${item.wordJa}::${item.weight ?: "null"}"
        }
    }

    private fun decodeItems(encoded: String): List<PersistedPromptItem> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split("|").mapNotNull { entry ->
            val parts = entry.split("::")
            if (parts.size != 4) return@mapNotNull null
            PersistedPromptItem(
                wordId = parts[0].toLongOrNull() ?: return@mapNotNull null,
                wordEn = parts[1],
                wordJa = parts[2],
                weight = parts[3].toFloatOrNull(),
            )
        }
    }
}
