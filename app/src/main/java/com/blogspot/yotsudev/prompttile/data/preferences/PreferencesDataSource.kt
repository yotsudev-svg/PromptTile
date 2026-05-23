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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Serializable
private data class UserTemplateDto(
    val name: String,
    val text: String,
    val isEnabled: Boolean = true,
)

@Serializable
private data class PersistedPromptItemDto(
    val wordId: Long,
    val wordEn: String,
    val wordJa: String,
    val weight: Float? = null,
)

// Json ビルダーは kotlinx.serialization.json.Json のトップレベル関数。
// lenient / ignoreUnknownKeys はどちらも json-jvm に含まれるプロパティで、
// 上記 import があれば解決される。
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_CONFIG = stringPreferencesKey("theme_config")
        val MOVE_TO_BACK = booleanPreferencesKey("move_to_back_on_copy")
        val USER_TEMPLATES = stringPreferencesKey("user_templates_v3")
        val DISABLED_DEFAULT_TEMPLATES = stringPreferencesKey("disabled_default_templates")
        val POSITIVE_ITEMS = stringPreferencesKey("positive_items_v2")
        val NEGATIVE_ITEMS = stringPreferencesKey("negative_items_v2")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeString = prefs[Keys.THEME_CONFIG] ?: ThemeConfig.FOLLOW_SYSTEM.name
        val themeConfig = runCatching {
            ThemeConfig.valueOf(themeString)
        }.getOrDefault(ThemeConfig.FOLLOW_SYSTEM)

        UserPreferences(
            themeConfig = themeConfig,
            moveToBackOnCopy = prefs[Keys.MOVE_TO_BACK] ?: false,
            userTemplates = decodeUserTemplates(prefs[Keys.USER_TEMPLATES] ?: ""),
            disabledDefaultTemplateNames = (prefs[Keys.DISABLED_DEFAULT_TEMPLATES] ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toSet(),
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
            val current = decodeUserTemplates(prefs[Keys.USER_TEMPLATES] ?: "").toMutableList()
            current.add(UserTemplate(name, text, true))
            prefs[Keys.USER_TEMPLATES] = encodeUserTemplates(current)
        }
    }

    suspend fun removeUserTemplate(name: String, text: String) {
        context.dataStore.edit { prefs ->
            val current = decodeUserTemplates(prefs[Keys.USER_TEMPLATES] ?: "").toMutableList()
            current.removeAll { it.name == name && it.text == text }
            prefs[Keys.USER_TEMPLATES] = encodeUserTemplates(current)
        }
    }

    suspend fun toggleUserTemplateEnabled(name: String, text: String) {
        context.dataStore.edit { prefs ->
            val current = decodeUserTemplates(prefs[Keys.USER_TEMPLATES] ?: "").map {
                if (it.name == name && it.text == text) it.copy(isEnabled = !it.isEnabled) else it
            }
            prefs[Keys.USER_TEMPLATES] = encodeUserTemplates(current)
        }
    }

    suspend fun toggleDefaultTemplateEnabled(name: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.DISABLED_DEFAULT_TEMPLATES] ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (current.contains(name)) current.remove(name) else current.add(name)
            prefs[Keys.DISABLED_DEFAULT_TEMPLATES] = current.joinToString(",")
        }
    }

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

    private fun encodeUserTemplates(list: List<UserTemplate>): String =
        json.encodeToString(list.map { UserTemplateDto(it.name, it.text, it.isEnabled) })

    private fun decodeUserTemplates(encoded: String): List<UserTemplate> {
        if (encoded.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<UserTemplateDto>>(encoded)
                .map { UserTemplate(it.name, it.text, it.isEnabled) }
        }.getOrDefault(emptyList())
    }

    private fun encodeItems(list: List<PersistedPromptItem>): String {
        if (list.isEmpty()) return ""
        return json.encodeToString(
            list.map { PersistedPromptItemDto(it.wordId, it.wordEn, it.wordJa, it.weight) }
        )
    }

    private fun decodeItems(encoded: String): List<PersistedPromptItem> {
        if (encoded.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<PersistedPromptItemDto>>(encoded)
                .map { PersistedPromptItem(it.wordId, it.wordEn, it.wordJa, it.weight) }
        }.getOrDefault(emptyList())
    }
}