package com.blogspot.yotsudev.prompttile.data.db

import android.util.Log
import org.json.JSONObject

// ─── カテゴリ系 ───────────────────────────────────────────────────────────────

data class SeedCategory(
    val id: Int,
    val parentId: Int = 0,
    val nameJa: String,
    val nameEn: String,
    val words: List<SeedWord>,
    val isNegative: Boolean = false,
)

data class SeedWord(
    val wordEn: String,
    val wordJa: String,
    /** 紐づくトッピンググループIDのリスト。 */
    val toppingGroupIds: List<Long> = emptyList(),
    /** 除外したいトッピングアイテムの valueEn リスト。 */
    val excludeToppingValues: List<String> = emptyList(),
)

// ─── トッピング系 ─────────────────────────────────────────────────────────────

data class SeedToppingGroup(
    val id: Int,
    val nameJa: String,
    val nameEn: String,
    val isPrefix: Boolean = true,
    val items: List<SeedToppingItem>,
)

data class SeedToppingItem(
    val valueEn: String,
    val nameJa: String,
    val colorHex: String? = null,
)

data class SeedParentCategory(
    val id: Int,
    val nameJa: String,
    val nameEn: String,
    val isNegative: Boolean = false,
)

// ─── 統合データクラス ─────────────────────────────────────────────────────────

data class SeedData(
    val parentCategories: List<SeedParentCategory>,
    val categories: List<SeedCategory>,
    val toppingGroups: List<SeedToppingGroup>,
)

// ─── パーサー ─────────────────────────────────────────────────────────────────

fun parseSeedData(json: String): SeedData {
    Log.d("SeedData", "parseSeedData started. JSON length: ${json.length}")
    val root = JSONObject(json)

    // 親カテゴリ
    val parentArray = if (root.has("parent_categories")) root.getJSONArray("parent_categories") else null
    val parentCategories = if (parentArray != null) {
        List(parentArray.length()) { i ->
            val obj = parentArray.getJSONObject(i)
            SeedParentCategory(
                id = obj.getInt("id"),
                nameJa = obj.getString("nameJa"),
                nameEn = obj.getString("nameEn"),
                isNegative = obj.optBoolean("isNegative", false)
            )
        }
    } else emptyList()

    // カテゴリ＆単語
    val categoriesArray = root.getJSONArray("categories")
    val categories = List(categoriesArray.length()) { i ->
        val catObj = categoriesArray.getJSONObject(i)
        val catId = catObj.optInt("id", -1)
        val catName = catObj.optString("nameEn", "Unknown")
        
        val wordsArray = catObj.getJSONArray("words")
        val words = List(wordsArray.length()) { j ->
            val wordObj = wordsArray.getJSONObject(j)
            try {
                SeedWord(
                    wordEn = wordObj.getString("wordEn"),
                    wordJa = wordObj.getString("wordJa"),
                    // 複数のトッピンググループIDに対応
                    toppingGroupIds = if (wordObj.has("toppingGroupIds")) {
                        val arr = wordObj.getJSONArray("toppingGroupIds")
                        List(arr.length()) { idx -> arr.getLong(idx) }
                    } else if (wordObj.has("toppingGroupId")) {
                        // 互換性のため古いキーも一応拾う
                        listOf(wordObj.getLong("toppingGroupId"))
                    } else emptyList(),
                    // 除外トッピング
                    excludeToppingValues = if (wordObj.has("excludeToppingValues")) {
                        val arr = wordObj.getJSONArray("excludeToppingValues")
                        List(arr.length()) { idx -> arr.getString(idx) }
                    } else emptyList()
                )
            } catch (e: Exception) {
                throw RuntimeException("Error parsing word at category $catName (ID: $catId), index $j: ${e.message}. JSON: $wordObj", e)
            }
        }
        SeedCategory(
            id         = catObj.getInt("id"),
            parentId   = catObj.optInt("parentId", 0),
            nameJa     = catObj.getString("nameJa"),
            nameEn     = catObj.getString("nameEn"),
            words      = words,
            isNegative = catObj.optBoolean("isNegative", false),
        )
    }

    // トッピンググループ＆アイテム
    val groupsArray = if (root.has("topping_groups"))
        root.getJSONArray("topping_groups") else null
    val toppingGroups = if (groupsArray != null) {
        List(groupsArray.length()) { i ->
            val groupObj = groupsArray.getJSONObject(i)
            val groupId = groupObj.optInt("id", -1)
            val groupName = groupObj.optString("nameEn", "Unknown")
            
            val itemsArray = groupObj.getJSONArray("items")
            val items = List(itemsArray.length()) { j ->
                val itemObj = itemsArray.getJSONObject(j)
                try {
                    SeedToppingItem(
                        valueEn  = itemObj.getString("valueEn"),
                        nameJa   = itemObj.getString("nameJa"),
                        colorHex = if (itemObj.has("colorHex"))
                            itemObj.getString("colorHex") else null,
                    )
                } catch (e: Exception) {
                    throw RuntimeException("Error parsing topping item at group $groupName (ID: $groupId), index $j: ${e.message}. JSON: $itemObj", e)
                }
            }
            SeedToppingGroup(
                id      = groupObj.getInt("id"),
                nameJa  = groupObj.getString("nameJa"),
                nameEn  = groupObj.getString("nameEn"),
                isPrefix = groupObj.optBoolean("isPrefix", true),
                items   = items,
            )
        }
    } else emptyList()

    return SeedData(
        parentCategories = parentCategories,
        categories = categories,
        toppingGroups = toppingGroups
    )
}
