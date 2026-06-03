package com.blogspot.yotsudev.prompttile.data.importer

import org.json.JSONObject

// ---- インポート用ドメインモデル ----------------------------------------

data class ImportCategory(
    val nameJa: String,
    val nameEn: String,
    val parentId: Long,
    val isNegative: Boolean,
    val words: List<ImportWord>,
)

data class ImportWord(
    val wordEn: String,
    val wordJa: String,
)

// ---- パース結果 --------------------------------------------------------

sealed class ImportParseResult {
    data class Success(val categories: List<ImportCategory>) : ImportParseResult()
    data class Failure(val message: String) : ImportParseResult()
}

// ---- パーサー本体 ------------------------------------------------------

/**
 * ユーザーが貼り付けたJSON文字列をパースして [ImportParseResult] を返す。
 *
 * 受け付けるフォーマット:
 * ```json
 * {
 *   "categories": [
 *     {
 *       "nameJa": "ポーズ追加",
 *       "nameEn": "Extra Poses",
 *       "parentId": 3,          // 省略時は 9（未分類親カテゴリ）
 *       "isNegative": false,    // 省略時は false
 *       "words": [
 *         { "wordEn": "hands on hips", "wordJa": "腰に手を当てる" }
 *       ]
 *     }
 *   ]
 * }
 * ```
 *
 * 単一カテゴリの場合は categories 配列なしでもOK:
 * ```json
 * {
 *   "nameJa": "ポーズ追加",
 *   "nameEn": "Extra Poses",
 *   "words": [...]
 * }
 * ```
 */
object JsonImportParser {

    // 未分類の親カテゴリID（seed_data.json の parent_categories より）
    private const val DEFAULT_PARENT_ID_POSITIVE = 9L
    private const val DEFAULT_PARENT_ID_NEGATIVE = 11L

    fun parse(json: String): ImportParseResult {
        if (json.isBlank()) return ImportParseResult.Failure("テキストが空です")

        return runCatching {
            val trimmed = json.trim()
            val root = JSONObject(trimmed)

            val categories = when {
                // パターンA: { "categories": [...] }
                root.has("categories") -> {
                    val arr = root.getJSONArray("categories")
                    List(arr.length()) { i -> parseCategory(arr.getJSONObject(i)) }
                }
                // パターンB: 単一カテゴリオブジェクト { "nameJa": ..., "words": [...] }
                root.has("words") -> listOf(parseCategory(root))
                else -> return ImportParseResult.Failure(
                    "\"categories\" または \"words\" キーが見つかりません"
                )
            }

            if (categories.isEmpty()) {
                return ImportParseResult.Failure("カテゴリが1件もありません")
            }

            val emptyNameCat = categories.firstOrNull { it.nameEn.isBlank() || it.nameJa.isBlank() }
            if (emptyNameCat != null) {
                return ImportParseResult.Failure("カテゴリ名（nameJa / nameEn）が空のエントリがあります")
            }

            ImportParseResult.Success(categories)
        }.getOrElse { e ->
            ImportParseResult.Failure("JSONの解析に失敗しました: ${e.message}")
        }
    }

    private fun parseCategory(obj: JSONObject): ImportCategory {
        val isNegative = obj.optBoolean("isNegative", false)
        val defaultParentId = if (isNegative) DEFAULT_PARENT_ID_NEGATIVE else DEFAULT_PARENT_ID_POSITIVE

        val wordsArray = obj.getJSONArray("words")
        val words = List(wordsArray.length()) { i ->
            val w = wordsArray.getJSONObject(i)
            ImportWord(
                wordEn = w.getString("wordEn").trim(),
                wordJa = w.optString("wordJa", "").trim(),
            )
        }.filter { it.wordEn.isNotBlank() } // 英語名が空の単語は除外

        return ImportCategory(
            nameJa     = obj.optString("nameJa", "").trim(),
            nameEn     = obj.optString("nameEn", "").trim(),
            parentId   = obj.optLong("parentId", defaultParentId),
            isNegative = isNegative,
            words      = words,
        )
    }
}