package com.blogspot.yotsudev.prompttile.util

import com.blogspot.yotsudev.prompttile.ui.main.PromptItem
import java.util.Locale

object PromptFormatter {

    /**
     * 単語をプロンプト用に整形する（重み付け対応）。
     */
    fun formatItem(item: PromptItem): String {
        return when {
            item.weight == null || item.weight == 1.0f -> item.wordEn
            else -> "(${item.wordEn}:${String.format(Locale.US, "%.1f", item.weight)})"
        }
    }

    /**
     * アイテムリストをカンマ区切りのプロンプト文字列に結合する。
     */
    fun formatPrompt(items: List<PromptItem>): String {
        return items.joinToString(", ") { formatItem(it) }
    }

    /**
     * カンマ区切りのテキストをパースして単語リストにする。
     */
    fun parsePromptText(text: String): List<String> {
        return text.split(",")
            .map { cleanWord(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * 単語から重み記号やカッコを除去し、基本形にする。
     */
    fun cleanWord(word: String): String {
        return word.replace(Regex("[()\\[\\]{}]"), "")
            .split(":")[0]
            .trim()
    }
}
