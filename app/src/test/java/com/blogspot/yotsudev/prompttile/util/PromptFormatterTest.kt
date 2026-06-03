package com.blogspot.yotsudev.prompttile.util

import com.blogspot.yotsudev.prompttile.ui.main.PromptItem
import com.blogspot.yotsudev.prompttile.ui.main.SelectedTopping
import org.junit.Assert.assertEquals
import org.junit.Test

class PromptFormatterTest {

    // ---- formatItem ----

    @Test
    fun `formatItem - 重みなしは単語そのまま返す`() {
        val item = PromptItem(wordId = 1, wordEn = "masterpiece", wordJa = "傑作")
        assertEquals("masterpiece", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - weight=1_0はnullと同じ扱いで括弧なし`() {
        val item = PromptItem(wordId = 1, wordEn = "masterpiece", wordJa = "", weight = 1.0f)
        assertEquals("masterpiece", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - 重みあり(1_2)は括弧とコロンで囲む`() {
        val item = PromptItem(wordId = 1, wordEn = "blue hair", wordJa = "", weight = 1.2f)
        assertEquals("(blue hair:1.2)", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - 重み1_15は末尾0を除去しない`() {
        val item = PromptItem(wordId = 1, wordEn = "smile", wordJa = "", weight = 1.15f)
        assertEquals("(smile:1.15)", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - 重み1_20は末尾0を除去して1_2になる`() {
        val item = PromptItem(wordId = 1, wordEn = "smile", wordJa = "", weight = 1.20f)
        assertEquals("(smile:1.2)", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - prefixトッピングありは単語の前に付く`() {
        val item = PromptItem(
            wordId = 1,
            wordEn = "dress",
            wordJa = "",
            selectedToppings = listOf(SelectedTopping(groupId = 1, valueEn = "red", isPrefix = true))
        )
        assertEquals("red dress", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - suffixトッピングありは単語の後に付く`() {
        val item = PromptItem(
            wordId = 1,
            wordEn = "sword",
            wordJa = "",
            selectedToppings = listOf(SelectedTopping(groupId = 7, valueEn = "with ribbon", isPrefix = false))
        )
        assertEquals("sword, with ribbon", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - prefix複数+suffixの組み合わせ`() {
        val item = PromptItem(
            wordId = 1,
            wordEn = "dress",
            wordJa = "",
            selectedToppings = listOf(
                SelectedTopping(groupId = 1, valueEn = "red", isPrefix = true),
                SelectedTopping(groupId = 2, valueEn = "silk", isPrefix = true),
                SelectedTopping(groupId = 7, valueEn = "with ribbon", isPrefix = false),
            )
        )
        // prefix同士は空白区切りで結合、suffixはカンマ区切りで結合 → "red silk dress, with ribbon"
        assertEquals("red silk dress, with ribbon", PromptFormatter.formatItem(item))
    }

    @Test
    fun `formatItem - トッピングあり+重みありは括弧の中にbaseTextが入る`() {
        val item = PromptItem(
            wordId = 1,
            wordEn = "dress",
            wordJa = "",
            weight = 1.3f,
            selectedToppings = listOf(SelectedTopping(groupId = 1, valueEn = "blue", isPrefix = true))
        )
        assertEquals("(blue dress:1.3)", PromptFormatter.formatItem(item))
    }

    // ---- formatPrompt ----

    @Test
    fun `formatPrompt - 空リストは空文字`() {
        assertEquals("", PromptFormatter.formatPrompt(emptyList()))
    }

    @Test
    fun `formatPrompt - 複数アイテムをカンマスペースで結合`() {
        val items = listOf(
            PromptItem(wordId = 1, wordEn = "masterpiece", wordJa = ""),
            PromptItem(wordId = 2, wordEn = "best quality", wordJa = ""),
            PromptItem(wordId = 3, wordEn = "1girl", wordJa = ""),
        )
        assertEquals("masterpiece, best quality, 1girl", PromptFormatter.formatPrompt(items))
    }

    @Test
    fun `formatPrompt - 重みつきアイテムが混在しても正しく結合`() {
        val items = listOf(
            PromptItem(wordId = 1, wordEn = "masterpiece", wordJa = ""),
            PromptItem(wordId = 2, wordEn = "blue hair", wordJa = "", weight = 1.2f),
        )
        assertEquals("masterpiece, (blue hair:1.2)", PromptFormatter.formatPrompt(items))
    }

    // ---- parsePromptText ----

    @Test
    fun `parsePromptText - カンマ区切りを分割してリスト化`() {
        val result = PromptFormatter.parsePromptText("masterpiece, best quality, 1girl")
        assertEquals(listOf("masterpiece", "best quality", "1girl"), result)
    }

    @Test
    fun `parsePromptText - 重み括弧を除去して単語を返す`() {
        val result = PromptFormatter.parsePromptText("masterpiece, (blue hair:1.2), smile")
        assertEquals(listOf("masterpiece", "blue hair", "smile"), result)
    }

    @Test
    fun `parsePromptText - 空文字は空リスト`() {
        assertEquals(emptyList<String>(), PromptFormatter.parsePromptText(""))
    }

    @Test
    fun `parsePromptText - 重複は除去される`() {
        val result = PromptFormatter.parsePromptText("smile, smile, blush")
        assertEquals(listOf("smile", "blush"), result)
    }

    // ---- cleanWord ----

    @Test
    fun `cleanWord - 括弧とコロン以降を除去`() {
        assertEquals("blue hair", PromptFormatter.cleanWord("(blue hair:1.2)"))
    }

    @Test
    fun `cleanWord - 前後の空白をトリム`() {
        assertEquals("smile", PromptFormatter.cleanWord("  smile  "))
    }

    @Test
    fun `cleanWord - 括弧なし通常単語はそのまま`() {
        assertEquals("masterpiece", PromptFormatter.cleanWord("masterpiece"))
    }

    @Test
    fun `cleanWord - 角括弧も除去`() {
        assertEquals("word", PromptFormatter.cleanWord("[word]"))
    }
}