package com.blogspot.yotsudev.prompttile.ui.saved

import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.preferences.ManagementFilterMode
import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import com.blogspot.yotsudev.prompttile.data.preferences.UserPreferences
import com.blogspot.yotsudev.prompttile.data.repository.PromptRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

    private lateinit var testDispatcher: TestDispatcher

    private lateinit var repository: PromptRepository
    private lateinit var dataSource: PreferencesDataSource
    private lateinit var viewModel: SavedViewModel

    private val prefsFlow     = MutableStateFlow(UserPreferences())
    private val promptsFlow   = MutableStateFlow<List<SavedPromptEntity>>(emptyList())

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        dataSource  = mockk(relaxed = true)

        every { dataSource.userPreferences } returns prefsFlow
        every { repository.savedPrompts }    returns promptsFlow

        viewModel = SavedViewModel(repository, dataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ----------------------------------------------------------------
    // 初期状態
    // ----------------------------------------------------------------

    @Test
    fun `初期状態 - savedPromptsは空リスト`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.savedPrompts.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `初期状態 - filterModeはALL`() = runTest {
        val job = launch { viewModel.filterMode.collect {} }
        advanceUntilIdle()

        assertEquals(ManagementFilterMode.ALL, viewModel.filterMode.value)
        job.cancel()
    }

    // ----------------------------------------------------------------
    // フィルター
    // ----------------------------------------------------------------

    @Test
    fun `ENABLED_ONLYフィルター - isEnabled=trueのみ表示`() = runTest {
        promptsFlow.value = listOf(
            makePrompt(id = 1L, title = "有効A",   isEnabled = true),
            makePrompt(id = 2L, title = "無効B",   isEnabled = false),
            makePrompt(id = 3L, title = "有効C",   isEnabled = true),
        )
        prefsFlow.value = UserPreferences(managementFilterMode = ManagementFilterMode.ENABLED_ONLY)

        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        val titles = viewModel.savedPrompts.value.map { it.title }
        assertEquals(listOf("有効A", "有効C"), titles)
        job.cancel()
    }

    @Test
    fun `DISABLED_ONLYフィルター - isEnabled=falseのみ表示`() = runTest {
        promptsFlow.value = listOf(
            makePrompt(id = 1L, title = "有効A",   isEnabled = true),
            makePrompt(id = 2L, title = "無効B",   isEnabled = false),
        )
        prefsFlow.value = UserPreferences(managementFilterMode = ManagementFilterMode.DISABLED_ONLY)

        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        val titles = viewModel.savedPrompts.value.map { it.title }
        assertEquals(listOf("無効B"), titles)
        job.cancel()
    }

    @Test
    fun `ALLフィルター - 全件表示される`() = runTest {
        promptsFlow.value = listOf(
            makePrompt(id = 1L, title = "有効A",   isEnabled = true),
            makePrompt(id = 2L, title = "無効B",   isEnabled = false),
        )
        prefsFlow.value = UserPreferences(managementFilterMode = ManagementFilterMode.ALL)

        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        assertEquals(2, viewModel.savedPrompts.value.size)
        job.cancel()
    }

    @Test
    fun `setFilterMode - DataSourceへの書き込みが呼ばれる`() = runTest {
        val job = launch { viewModel.filterMode.collect {} }
        advanceUntilIdle()

        viewModel.setFilterMode(ManagementFilterMode.DISABLED_ONLY)
        advanceUntilIdle()

        coVerify { dataSource.updateManagementFilterMode(ManagementFilterMode.DISABLED_ONLY) }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // 削除
    // ----------------------------------------------------------------

    @Test
    fun `delete - isDefault=falseのプロンプトはdeletPromptが呼ばれる`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        val prompt = makePrompt(id = 1L, title = "削除対象", isDefault = false)
        viewModel.delete(prompt)
        advanceUntilIdle()

        coVerify { repository.deletePrompt(prompt) }
        job.cancel()
    }

    @Test
    fun `delete - isDefault=trueのプロンプトはdeletePromptが呼ばれない`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        val prompt = makePrompt(id = 1L, title = "デフォルト", isDefault = true)
        viewModel.delete(prompt)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.deletePrompt(any()) }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // 更新・有効/無効切り替え
    // ----------------------------------------------------------------

    @Test
    fun `updatePrompt - repositoryのupdatePromptが呼ばれる`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        val updated = makePrompt(id = 1L, title = "更新後タイトル")
        viewModel.updatePrompt(updated)
        advanceUntilIdle()

        coVerify { repository.updatePrompt(updated) }
        job.cancel()
    }

    @Test
    fun `togglePromptEnabled - isEnabledが反転して保存される`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        val prompt = makePrompt(id = 1L, title = "テスト", isEnabled = true)
        viewModel.togglePromptEnabled(prompt)
        advanceUntilIdle()

        coVerify { repository.updatePrompt(match { !it.isEnabled }) }
        job.cancel()
    }

    @Test
    fun `togglePromptEnabled - isEnabled=falseのものはtrueになる`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        val prompt = makePrompt(id = 1L, title = "テスト", isEnabled = false)
        viewModel.togglePromptEnabled(prompt)
        advanceUntilIdle()

        coVerify { repository.updatePrompt(match { it.isEnabled }) }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // 手動追加
    // ----------------------------------------------------------------

    @Test
    fun `addManualPrompt - savePromptが呼ばれる`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        viewModel.addManualPrompt(
            title        = "マイプロンプト",
            positiveText = "masterpiece, 1girl",
            negativeText = "low quality",
        )
        advanceUntilIdle()

        coVerify {
            repository.savePrompt(
                match {
                    it.title        == "マイプロンプト" &&
                            it.promptText   == "masterpiece, 1girl" &&
                            it.negativeText == "low quality"
                }
            )
        }
        job.cancel()
    }

    @Test
    fun `addManualPrompt - タイトルが空の場合は日時文字列が使われる`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        viewModel.addManualPrompt(
            title        = "",
            positiveText = "masterpiece",
            negativeText = "",
        )
        advanceUntilIdle()

        // タイトルが空でないこと（日時文字列が補完される）を確認
        coVerify {
            repository.savePrompt(match { it.title.isNotBlank() })
        }
        job.cancel()
    }

    @Test
    fun `addManualPrompt - positiveとnegativeが両方空の場合は保存されない`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        viewModel.addManualPrompt(title = "タイトル", positiveText = "", negativeText = "")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.savePrompt(any()) }
        job.cancel()
    }

    @Test
    fun `addManualPrompt - positiveTextがあればregisterNewWordsFromTextも呼ばれる`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        viewModel.addManualPrompt(
            title        = "テスト",
            positiveText = "masterpiece, 1girl",
            negativeText = "",
        )
        advanceUntilIdle()

        coVerify {
            repository.registerNewWordsFromText(
                text       = "masterpiece, 1girl",
                isNegative = false,
            )
        }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // 並び替え
    // ----------------------------------------------------------------

    @Test
    fun `reorderPrompts - ドラッグ中に並び順が変わる`() = runTest {
        promptsFlow.value = listOf(
            makePrompt(id = 1L, title = "A"),
            makePrompt(id = 2L, title = "B"),
            makePrompt(id = 3L, title = "C"),
        )
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        viewModel.reorderPrompts(from = 0, to = 2)
        advanceUntilIdle()

        val titles = viewModel.savedPrompts.value.map { it.title }
        assertEquals(listOf("B", "C", "A"), titles)
        job.cancel()
    }

    @Test
    fun `persistOrder - updateSavedPromptsが新しいsortOrderで呼ばれる`() = runTest {
        promptsFlow.value = listOf(
            makePrompt(id = 1L, title = "A"),
            makePrompt(id = 2L, title = "B"),
        )
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        viewModel.reorderPrompts(from = 0, to = 1)
        advanceUntilIdle()
        viewModel.persistOrder()
        advanceUntilIdle()

        coVerify {
            repository.updateSavedPrompts(
                match { list ->
                    list.find { it.title == "B" }?.sortOrder == 0 &&
                            list.find { it.title == "A" }?.sortOrder == 1
                }
            )
        }
        job.cancel()
    }

    @Test
    fun `resetOrder - repositoryのresetSavedPromptOrderが呼ばれる`() = runTest {
        val job = launch { viewModel.savedPrompts.collect {} }
        advanceUntilIdle()

        viewModel.resetOrder()
        advanceUntilIdle()

        coVerify { repository.resetSavedPromptOrder() }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // ヘルパー
    // ----------------------------------------------------------------

    private fun makePrompt(
        id: Long,
        title: String,
        positiveText: String = "masterpiece",
        negativeText: String = "",
        isDefault: Boolean   = false,
        isEnabled: Boolean   = true,
        sortOrder: Int       = 0,
    ) = SavedPromptEntity(
        id           = id,
        title        = title,
        promptText   = positiveText,
        negativeText = negativeText,
        isDefault    = isDefault,
        isEnabled    = isEnabled,
        sortOrder    = sortOrder,
    )
}