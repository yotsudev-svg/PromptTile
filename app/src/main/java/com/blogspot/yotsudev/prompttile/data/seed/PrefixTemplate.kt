package com.blogspot.yotsudev.prompttile.data.seed

import org.json.JSONObject

/**
 * プレフィックステンプレートのデータクラス。
 *
 * [isDefault] で「デフォルト（削除不可）」と「ユーザー作成（削除可能）」を区別する。
 * デフォルトは seed_data.json から読み込み、ユーザー作成は DataStore に保存する。
 */
data class PrefixTemplate(
    val name: String,
    val text: String,
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
)

/** seed_data.json の prefix_templates セクションをパースする */
fun parsePrefixTemplates(json: String): List<PrefixTemplate> {
    val root = JSONObject(json)
    val array = root.getJSONArray("prefix_templates")
    return List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        PrefixTemplate(
            name      = obj.getString("name"),
            text      = obj.getString("text"),
            isDefault = true,
        )
    }
}