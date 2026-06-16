package com.blogspot.yotsudev.prompttile.data.importer

import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * データベースのエンティティを [JsonImportParser] が読み取れる形式の JSON 文字列に変換する。
 */
object JsonExporter {

    fun export(
        categories: List<CategoryEntity>,
        allWords: List<PromptWordEntity>
    ): String {
        val root = JSONObject()
        val categoriesArray = JSONArray()

        val wordsByCategory = allWords.groupBy { it.categoryId }

        categories.forEach { category ->
            val categoryJson = JSONObject().apply {
                put("nameJa", category.nameJa)
                put("nameEn", category.nameEn)
                put("parentId", category.parentId)
                put("isNegative", category.isNegative)

                val wordsArray = JSONArray()
                wordsByCategory[category.id]?.forEach { word ->
                    val wordJson = JSONObject().apply {
                        put("wordEn", word.wordEn)
                        put("wordJa", word.wordJa)
                    }
                    wordsArray.put(wordJson)
                }
                put("words", wordsArray)
            }
            categoriesArray.put(categoryJson)
        }

        root.put("categories", categoriesArray)
        return root.toString(2) // インデント付きで整形
    }
}
