package com.blogspot.yotsudev.prompttile

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable
data class AppConfigJson(
    val categories: List<CategoryData>
)

@Serializable
data class CategoryData(
    val id: Int,
    val parentId: Int,
    val nameJa: String,
    val nameEn: String,
    val words: List<WordItem>
)

@Serializable
data class WordItem(
    val wordEn: String,
    val wordJa: String
)

class TagValidatorTest {

    @Test
    fun validateDanbooruTags() = runBlocking {
        // ----------------------------------------------------
        // 設定項目：チェックしたい categories の ID を指定します
        // ----------------------------------------------------
        val targetCategoryId = 2 // 例: 2 = アートスタイル, 1 = クオリティ

        val assetPath = "C:\\Users\\faity\\AndroidStudioProjects\\PromptTile\\app\\src\\main\\assets"
        // ※ 実際のファイル名（例: app_config.json）に合わせて書き換えてください
        val jsonFile = File(assetPath, "seed_data.json")

        if (!jsonFile.exists()) {
            println("❌ ファイルが見つかりません: ${jsonFile.absolutePath}")
            return@runBlocking
        }

        // JSONファイルの読み込みとパース
        val jsonText = jsonFile.readText()
        val jsonConfig = Json { ignoreUnknownKeys = true } // 余計な項目は無視する設定

        // ルートのオブジェクト（AppConfigJson）としてパース
        val appConfig = jsonConfig.decodeFromString<AppConfigJson>(jsonText)

        // 指定されたIDのカテゴリを検索
        val targetCategory = appConfig.categories.find { it.id == targetCategoryId }

        if (targetCategory == null) {
            println("❌ 指定されたカテゴリID (${targetCategoryId}) がJSON内に見つかりませんでした。")
            return@runBlocking
        }

        val testWords = targetCategory.words

        println("🔍 【ID: $targetCategoryId】「${targetCategory.nameJa}」の先頭 ${testWords.size} 件のチェックを開始します。\n")

        val invalidWords = mutableListOf<WordItem>()

        // 10件のループを実行
        testWords.forEachIndexed { index, wordItem ->
            // スペースをすべてアンダースコアに自動変換してからAPIに投げる
            val tagToSearch = wordItem.wordEn.replace(" ", "_")

            // Danbooru APIへの問い合わせ
            val isExist = checkTagExistsOnDanbooru(tagToSearch)

            if (isExist) {
                println("[${index + 1}/${testWords.size}] ○ 存在します: $tagToSearch (${wordItem.wordJa})")
            } else {
                println("[${index + 1}/${testWords.size}] ❌ 存在しません: $tagToSearch (${wordItem.wordJa})")
                invalidWords.add(wordItem)
            }

            // 1.2秒待機
            delay(1200)
        }

        // 結果のまとめ表示
        println("\n==========================================")
        println("🎉 ID: ${targetCategoryId} のテストチェックが完了しました！")
        println("存在しなかった（修正が必要な）タグ一覧:")
        if (invalidWords.isEmpty()) {
            println("✨ テストした10件はすべてDanbooruに存在していました！")
        } else {
            invalidWords.forEach {
                println("・ \"wordEn\": \"${it.wordEn}\" (日本語: ${it.wordJa})")
            }
        }
        println("==========================================")
    }

    private fun checkTagExistsOnDanbooru(tagName: String): Boolean {
        return try {
            val encodedName = URLEncoder.encode(tagName, "UTF-8")
            val urlString = "https://danbooru.donmai.us/tags.json?search[name]=$encodedName"
            val url = URL(urlString)

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "PromptTileApp/1.0 (faity)")

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                responseText.trim() != "[]"
            } else {
                false
            }
        } catch (e: Exception) {
            println("⚠️ 通信エラー ($tagName): ${e.message}")
            false
        }
    }
}