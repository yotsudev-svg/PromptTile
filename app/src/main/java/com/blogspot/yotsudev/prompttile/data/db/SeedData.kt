package com.blogspot.yotsudev.prompttile.data.db

import org.json.JSONObject

data class SeedCategory(
    val id: Int,
    val nameJa: String,
    val nameEn: String,
    val words: List<SeedWord>,
    /** ネガティブ専用カテゴリかどうか。JSONに isNegative キーがなければ false */
    val isNegative: Boolean = false,
)

data class SeedWord(
    val wordEn: String,
    val wordJa: String,
)

fun parseSeedData(json: String): List<SeedCategory> {
    val root = JSONObject(json)
    val categoriesArray = root.getJSONArray("categories")

    return List(categoriesArray.length()) { i ->
        val catObj = categoriesArray.getJSONObject(i)
        val wordsArray = catObj.getJSONArray("words")

        val words = List(wordsArray.length()) { j ->
            val wordObj = wordsArray.getJSONObject(j)
            SeedWord(
                wordEn = wordObj.getString("wordEn"),
                wordJa = wordObj.getString("wordJa"),
            )
        }

        SeedCategory(
            id         = catObj.getInt("id"),
            nameJa     = catObj.getString("nameJa"),
            nameEn     = catObj.getString("nameEn"),
            words      = words,
            // キーが存在しない既存カテゴリは false として扱う
            isNegative = catObj.optBoolean("isNegative", false),
        )
    }
}