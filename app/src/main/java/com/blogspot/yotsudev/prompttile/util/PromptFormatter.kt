package com.blogspot.yotsudev.prompttile.util

import com.blogspot.yotsudev.prompttile.ui.main.PromptItem
import java.util.Locale

object PromptFormatter {

    /**
     * 単語をプロンプト用に整形する（重み付け対応）。
     *
     * 0.05 刻みの微調整に対応しつつ、無駄な末尾の 0 を省く。
     * 例: 1.20 -> 1.2, 1.15 -> 1.15
     */
    fun formatItem(item: PromptItem): String {
        val weight = item.weight
        return when {
            weight == null || weight == 1.0f -> item.baseText
            else -> {
                // 小数点以下2桁でフォーマットし、末尾の0を消す
                // 1.20 -> 1.2, 1.15 -> 1.15
                val weightStr = String.format(Locale.US, "%.2f", weight)
                    .replace(Regex("0+$"), "") // 末尾の0を削除
                    .replace(Regex("\\.$"), ".0") // もし 1. になったら 1.0 に戻す（念の為）
                "(${item.baseText}:${weightStr})"
            }
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
