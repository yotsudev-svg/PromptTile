package com.blogspot.yotsudev.prompttile.ui.main

import android.content.Context
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.UserPreferences
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PromptViewModel のユニットテスト。
 *
 * 修正ポイント:
 * 1. StandardTestDispatcher の生成を setUp() 内（setMain後）に移動し、
 *    フィールド初期化時の Looper クラッシュを回避。
 * 2. uiState は SharingStarted.WhileSubscribed のため、
 *    テスト内で launch { uiState.collect {} } して購読者を作ってから操作する。
 *    購読後 advanceUntilIdle() を呼ぶことで StateFlow の結合処理を全て完了させる。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PromptViewModelTest {

    // フィールド宣言時に StandardTestDispatcher() を呼ばない（Looperクラッシュ防止）
    private lateinit var testDispatcher: TestDispatcher

    private lateinit var repository: PromptRepository
    private lateinit var dataSource: PreferencesDataSource
    private lateinit var context: Context
    private lateinit var viewModel: PromptViewModel

    private val prefsFlow = MutableStateFlow(UserPreferences())

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        dataSource  = mockk(relaxed = true)
        context     = mockk(relaxed = true)

        // ✨ 修正: PrefixTemplate.kt の root.getJSONArray("prefix_templates") に合わせる
        val validJsonString = """
        {
            "prefix_templates": []
        }
    """.trimIndent()

        every { context.assets } returns mockk {
            every { open(any()) } returns validJsonString.byteInputStream()
        }

        every { dataSource.userPreferences } returns prefsFlow
        every { repository.observeParentCategories(any()) } returns flowOf(emptyList())
        every { repository.observeCategoriesByParent(any(), any()) } returns flowOf(emptyList())
        every { repository.visibleWordsByCategory(any()) } returns flowOf(emptyList())
        every { repository.searchWords(any()) } returns flowOf(emptyList())
        every { repository.savedPrompts } returns flowOf(emptyList())

        viewModel = PromptViewModel(repository, dataSource, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // unmockkAll() があれば残しておいても、消してもどちらでも大丈夫です
    }

    // ---- toggleWord ----

    @Test
    fun `toggleWord - 追加できる`() = runTest {
        // uiState の WhileSubscribed を起動するため購読者を作る
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "masterpiece"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.positiveItems.size)
        assertEquals("masterpiece", viewModel.uiState.value.positiveItems[0].wordEn)
        job.cancel()
    }

    @Test
    fun `toggleWord - 同じ単語を再タップすると削除される`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val word = makeWord(id = 1, en = "masterpiece")
        viewModel.toggleWord(word)
        advanceUntilIdle()
        viewModel.toggleWord(word)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.positiveItems.isEmpty())
        job.cancel()
    }

    @Test
    fun `toggleWord - negativeモードではnegativeItemsに追加される`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.switchMode(PromptMode.NEGATIVE)
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 2, en = "blurry"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.positiveItems.isEmpty())
        assertEquals(1, viewModel.uiState.value.negativeItems.size)
        job.cancel()
    }

    // ---- Undo / Redo ----

    @Test
    fun `undo - toggleWordの操作を元に戻せる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "masterpiece"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.positiveItems.size)

        viewModel.undo()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.positiveItems.isEmpty())
        job.cancel()
    }

    @Test
    fun `redo - undoの後にredoで再適用できる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "masterpiece"))
        advanceUntilIdle()
        viewModel.undo()
        advanceUntilIdle()
        viewModel.redo()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.positiveItems.size)
        job.cancel()
    }

    @Test
    fun `canUndo - 初期状態はfalse、追加後はtrue`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canUndo)

        viewModel.toggleWord(makeWord(id = 1, en = "smile"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canUndo)
        job.cancel()
    }

    @Test
    fun `canRedo - 初期状態はfalse、undo後はtrue`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "smile"))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canRedo)

        viewModel.undo()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canRedo)
        job.cancel()
    }

    // ---- removeItem / clearAll ----

    @Test
    fun `removeItem - 特定アイテムを削除できる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "masterpiece"))
        viewModel.toggleWord(makeWord(id = 2, en = "1girl"))
        advanceUntilIdle()

        val target = viewModel.uiState.value.positiveItems.first { it.wordEn == "masterpiece" }
        viewModel.removeItem(target)
        advanceUntilIdle()

        val remaining = viewModel.uiState.value.positiveItems
        assertEquals(1, remaining.size)
        assertEquals("1girl", remaining[0].wordEn)
        job.cancel()
    }

    @Test
    fun `clearAll - 全アイテムが削除される`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "masterpiece"))
        viewModel.toggleWord(makeWord(id = 2, en = "1girl"))
        advanceUntilIdle()

        viewModel.clearAll()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.positiveItems.isEmpty())
        job.cancel()
    }

    // ---- setWeight ----

    @Test
    fun `setWeight - 重みを設定できる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "smile"))
        advanceUntilIdle()

        val item = viewModel.uiState.value.positiveItems.first()
        viewModel.setWeight(item, 1.2f)
        advanceUntilIdle()

        assertEquals(1.2f, viewModel.uiState.value.positiveItems.first().weight)
        job.cancel()
    }

    @Test
    fun `setWeight - nullを渡すと重みが解除される`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "smile"))
        advanceUntilIdle()

        val item = viewModel.uiState.value.positiveItems.first()
        viewModel.setWeight(item, 1.2f)
        advanceUntilIdle()

        val weighted = viewModel.uiState.value.positiveItems.first()
        viewModel.setWeight(weighted, null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.positiveItems.first().weight)
        job.cancel()
    }

    // ---- moveItem ----

    @Test
    fun `moveItem - インデックス0から2へ移動する`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "A"))
        viewModel.toggleWord(makeWord(id = 2, en = "B"))
        viewModel.toggleWord(makeWord(id = 3, en = "C"))
        advanceUntilIdle()

        viewModel.moveItem(from = 0, to = 2)
        advanceUntilIdle()

        val words = viewModel.uiState.value.positiveItems.map { it.wordEn }
        assertEquals(listOf("B", "C", "A"), words)
        job.cancel()
    }

    // ---- addTemplateItems ----

    @Test
    fun `addTemplateItems - テキストを解析して単語を追加する`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.addTemplateItems("masterpiece, best quality, anime artwork")
        advanceUntilIdle()

        val words = viewModel.uiState.value.positiveItems.map { it.wordEn }
        assertTrue("masterpiece" in words)
        assertTrue("best quality" in words)
        assertTrue("anime artwork" in words)
        job.cancel()
    }

    @Test
    fun `addTemplateItems - 既存単語との重複は追加されない`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "masterpiece"))
        advanceUntilIdle()

        viewModel.addTemplateItems("masterpiece, best quality")
        advanceUntilIdle()

        val count = viewModel.uiState.value.positiveItems.count { it.wordEn == "masterpiece" }
        assertEquals(1, count)
        job.cancel()
    }

    // ---- 副作用検証 ----

    @Test
    fun `toggleWord - DataSourceのupdatePersistedItemsが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleWord(makeWord(id = 1, en = "masterpiece"))
        advanceUntilIdle()

        coVerify(atLeast = 1) { dataSource.updatePersistedItems(any(), any()) }
        job.cancel()
    }

    // ---- ヘルパー ----

    private fun makeWord(id: Long, en: String) = PromptWordEntity(
        id         = id,
        categoryId = 1L,
        wordEn     = en,
        wordJa     = "",
    )
}