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
private data class PersistedPromptItemDto(
    val wordId: Long,
    val wordEn: String,
    val wordJa: String,
    val weight: Float? = null,
    val toppingGroupIds: List<Long> = emptyList(),
    val selectedToppings: List<PersistedSelectedToppingDto> = emptyList(),
    val excludeToppingValues: List<String> = emptyList(),
    val tags: String? = null,
)

@Serializable
private data class PersistedSelectedToppingDto(
    val groupId: Long,
    val valueEn: String,
    val isPrefix: Boolean,
    val priority: Int = 999,
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
        val MANAGEMENT_FILTER_MODE = stringPreferencesKey("management_filter_mode")
        val MOVE_TO_BACK = booleanPreferencesKey("move_to_back_on_copy")
        val POSITIVE_ITEMS = stringPreferencesKey("positive_items_v2")
        val NEGATIVE_ITEMS = stringPreferencesKey("negative_items_v2")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeString = prefs[Keys.THEME_CONFIG] ?: ThemeConfig.FOLLOW_SYSTEM.name
        val themeConfig = runCatching {
            ThemeConfig.valueOf(themeString)
        }.getOrDefault(ThemeConfig.FOLLOW_SYSTEM)

        val filterString = prefs[Keys.MANAGEMENT_FILTER_MODE] ?: ManagementFilterMode.ALL.name
        val filterMode = runCatching {
            ManagementFilterMode.valueOf(filterString)
        }.getOrDefault(ManagementFilterMode.ALL)

        UserPreferences(
            themeConfig = themeConfig,
            managementFilterMode = filterMode,
            moveToBackOnCopy = prefs[Keys.MOVE_TO_BACK] ?: false,
            persistedPositiveItems = decodeItems(prefs[Keys.POSITIVE_ITEMS] ?: ""),
            persistedNegativeItems = decodeItems(prefs[Keys.NEGATIVE_ITEMS] ?: ""),
        )
    }

    suspend fun updateThemeConfig(config: ThemeConfig) {
        context.dataStore.edit { it[Keys.THEME_CONFIG] = config.name }
    }

    suspend fun updateManagementFilterMode(mode: ManagementFilterMode) {
        context.dataStore.edit { it[Keys.MANAGEMENT_FILTER_MODE] = mode.name }
    }

    suspend fun updateMoveToBack(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MOVE_TO_BACK] = enabled }
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

    private fun encodeItems(list: List<PersistedPromptItem>): String {
        if (list.isEmpty()) return ""
        return json.encodeToString(
            list.map { item ->
                PersistedPromptItemDto(
                    item.wordId,
                    item.wordEn,
                    item.wordJa,
                    item.weight,
                    item.toppingGroupIds,
                    item.selectedToppings.map {
                        PersistedSelectedToppingDto(it.groupId, it.valueEn, it.isPrefix, it.priority)
                    },
                    item.excludeToppingValues,
                    item.tags
                )
            }
        )
    }

    private fun decodeItems(encoded: String): List<PersistedPromptItem> {
        if (encoded.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<PersistedPromptItemDto>>(encoded)
                .map { item ->
                    PersistedPromptItem(
                        item.wordId,
                        item.wordEn,
                        item.wordJa,
                        item.weight,
                        item.toppingGroupIds,
                        item.selectedToppings.map {
                            PersistedSelectedTopping(it.groupId, it.valueEn, it.isPrefix, it.priority)
                        },
                        item.excludeToppingValues,
                        item.tags
                    )
                }
        }.getOrDefault(emptyList())
    }
}