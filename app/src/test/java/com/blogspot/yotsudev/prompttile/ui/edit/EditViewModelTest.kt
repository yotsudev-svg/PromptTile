package com.blogspot.yotsudev.prompttile.ui.edit

import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordWithCategory
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
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {

    private lateinit var testDispatcher: TestDispatcher

    private lateinit var repository: PromptRepository
    private lateinit var dataSource: PreferencesDataSource
    private lateinit var viewModel: EditViewModel

    private val prefsFlow = MutableStateFlow(UserPreferences())

    // テスト用カテゴリ・単語のバッキングフロー（テスト内から値を差し替えられる）
    private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private val wordsFlow      = MutableStateFlow<List<PromptWordEntity>>(emptyList())
    private val allWordsWithCategoryFlow = MutableStateFlow<List<PromptWordWithCategory>>(emptyList())

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        dataSource  = mockk(relaxed = true)

        every { dataSource.userPreferences }     returns prefsFlow
        every { repository.allCategories }       returns categoriesFlow
        every { repository.allWordsByCategory(any()) } returns wordsFlow
        every { repository.allWordsWithCategory } returns allWordsWithCategoryFlow

        viewModel = EditViewModel(repository, dataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ----------------------------------------------------------------
    // filterMode
    // ----------------------------------------------------------------

    @Test
    fun `初期filterModeはALL`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(ManagementFilterMode.ALL, viewModel.uiState.value.filterMode)
        job.cancel()
    }

    @Test
    fun `setFilterMode - ENABLED_ONLYに変更できる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setFilterMode(ManagementFilterMode.ENABLED_ONLY)
        advanceUntilIdle()

        // DataSource への書き込みが呼ばれる
        coVerify { dataSource.updateManagementFilterMode(ManagementFilterMode.ENABLED_ONLY) }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // toggleExpand
    // ----------------------------------------------------------------

    @Test
    fun `toggleExpand - カテゴリを展開できる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleExpand(categoryId = 1L)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.expandedCategoryId)
        job.cancel()
    }

    @Test
    fun `toggleExpand - 同じカテゴリを再タップすると折りたたまれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleExpand(categoryId = 1L)
        advanceUntilIdle()
        viewModel.toggleExpand(categoryId = 1L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.expandedCategoryId)
        job.cancel()
    }

    @Test
    fun `toggleExpand - 別のカテゴリをタップすると展開対象が切り替わる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleExpand(categoryId = 1L)
        advanceUntilIdle()
        viewModel.toggleExpand(categoryId = 2L)
        advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.expandedCategoryId)
        job.cancel()
    }

    // ----------------------------------------------------------------
    // カテゴリ CRUD
    // ----------------------------------------------------------------

    @Test
    fun `addCategory - repositoryのinsertCategoryが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.addCategory(ja = "テスト", en = "Test")
        advanceUntilIdle()

        coVerify {
            repository.insertCategory(
                match { it.nameJa == "テスト" && it.nameEn == "Test" }
            )
        }
        job.cancel()
    }

    @Test
    fun `updateCategory - repositoryのupdateCategoryが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val original = makeCategory(id = 1L, ja = "旧名", en = "OldName")
        viewModel.updateCategory(original, ja = "新名", en = "NewName")
        advanceUntilIdle()

        coVerify {
            repository.updateCategory(
                match { it.id == 1L && it.nameJa == "新名" && it.nameEn == "NewName" }
            )
        }
        job.cancel()
    }

    @Test
    fun `deleteCategory - repositoryのdeleteCategoryが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val category = makeCategory(id = 1L, ja = "テスト", en = "Test")
        viewModel.deleteCategory(category)
        advanceUntilIdle()

        coVerify { repository.deleteCategory(category) }
        job.cancel()
    }

    @Test
    fun `toggleCategoryVisibility - repositoryのtogglerが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val category = makeCategory(id = 1L, ja = "テスト", en = "Test")
        viewModel.toggleCategoryVisibility(category)
        advanceUntilIdle()

        coVerify { repository.toggleCategoryVisibility(category) }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // カテゴリ並び替え
    // ----------------------------------------------------------------

    @Test
    fun `reorderCategories - ドラッグ中に並び順が変わる`() = runTest {
        // DB から3件流す
        categoriesFlow.value = listOf(
            makeCategory(id = 1L, ja = "A", en = "A"),
            makeCategory(id = 2L, ja = "B", en = "B"),
            makeCategory(id = 3L, ja = "C", en = "C"),
        )
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // index 0 (A) を index 2 (C) へ移動
        viewModel.reorderCategories(from = 0, to = 2)
        advanceUntilIdle()

        val names = viewModel.uiState.value.categories.map { it.nameEn }
        assertEquals(listOf("B", "C", "A"), names)
        job.cancel()
    }

    @Test
    fun `persistCategoryOrder - updateCategoriesが新しいsortOrderで呼ばれる`() = runTest {
        categoriesFlow.value = listOf(
            makeCategory(id = 1L, ja = "A", en = "A"),
            makeCategory(id = 2L, ja = "B", en = "B"),
        )
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.reorderCategories(from = 0, to = 1)
        advanceUntilIdle()
        viewModel.persistCategoryOrder()
        advanceUntilIdle()

        coVerify {
            repository.updateCategories(
                match { list ->
                    // B(元index1)が sortOrder=0、A(元index0)が sortOrder=1 になっている
                    list.find { it.nameEn == "B" }?.sortOrder == 0 &&
                            list.find { it.nameEn == "A" }?.sortOrder == 1
                }
            )
        }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // 単語 CRUD
    // ----------------------------------------------------------------

    @Test
    fun `addWord - repositoryのinsertWordが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.addWord(categoryId = 1L, en = "smile", ja = "笑顔")
        advanceUntilIdle()

        coVerify {
            repository.insertWord(
                match { it.categoryId == 1L && it.wordEn == "smile" && it.wordJa == "笑顔" }
            )
        }
        job.cancel()
    }

    @Test
    fun `updateWord - repositoryのupdateWordが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val word = makeWord(id = 1L, categoryId = 1L, en = "smile", ja = "笑顔")
        viewModel.updateWord(word, en = "grin", ja = "にやり", newCategoryId = null)
        advanceUntilIdle()

        coVerify {
            repository.updateWord(
                match { it.id == 1L && it.wordEn == "grin" && it.wordJa == "にやり" }
            )
        }
        job.cancel()
    }

    @Test
    fun `updateWord - newCategoryIdを渡すとカテゴリが変わる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val word = makeWord(id = 1L, categoryId = 1L, en = "smile", ja = "笑顔")
        viewModel.updateWord(word, en = "smile", ja = "笑顔", newCategoryId = 99L)
        advanceUntilIdle()

        coVerify {
            repository.updateWord(match { it.categoryId == 99L })
        }
        job.cancel()
    }

    @Test
    fun `deleteWord - repositoryのdeleteWordが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val word = makeWord(id = 1L, categoryId = 1L, en = "smile", ja = "笑顔")
        viewModel.deleteWord(word)
        advanceUntilIdle()

        coVerify { repository.deleteWord(word) }
        job.cancel()
    }

    @Test
    fun `toggleWordVisibility - repositoryのtogglerが呼ばれる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val word = makeWord(id = 1L, categoryId = 1L, en = "smile", ja = "笑顔")
        viewModel.toggleWordVisibility(word)
        advanceUntilIdle()

        coVerify { repository.toggleWordVisibility(word) }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // 単語並び替え
    // ----------------------------------------------------------------

    @Test
    fun `reorderWords - ドラッグ中に並び順が変わる`() = runTest {
        // カテゴリを展開してから単語を流す
        wordsFlow.value = listOf(
            makeWord(id = 1L, categoryId = 1L, en = "A", ja = "A"),
            makeWord(id = 2L, categoryId = 1L, en = "B", ja = "B"),
            makeWord(id = 3L, categoryId = 1L, en = "C", ja = "C"),
        )
        val job = launch { viewModel.uiState.collect {} }
        viewModel.toggleExpand(categoryId = 1L)
        advanceUntilIdle()

        viewModel.reorderWords(from = 0, to = 2)
        advanceUntilIdle()

        val names = viewModel.uiState.value.wordsInExpanded.map { it.wordEn }
        assertEquals(listOf("B", "C", "A"), names)
        job.cancel()
    }

    @Test
    fun `persistWordOrder - updateWordsが新しいsortOrderで呼ばれる`() = runTest {
        wordsFlow.value = listOf(
            makeWord(id = 1L, categoryId = 1L, en = "A", ja = "A"),
            makeWord(id = 2L, categoryId = 1L, en = "B", ja = "B"),
        )
        val job = launch { viewModel.uiState.collect {} }
        viewModel.toggleExpand(categoryId = 1L)
        advanceUntilIdle()

        viewModel.reorderWords(from = 0, to = 1)
        advanceUntilIdle()
        viewModel.persistWordOrder()
        advanceUntilIdle()

        coVerify {
            repository.updateWords(
                match { list ->
                    list.find { it.wordEn == "B" }?.sortOrder == 0 &&
                            list.find { it.wordEn == "A" }?.sortOrder == 1
                }
            )
        }
        job.cancel()
    }

    // ----------------------------------------------------------------
    // フィルター（DISABLED_ONLY）
    // ----------------------------------------------------------------

    @Test
    fun `DISABLED_ONLYフィルター - 非表示カテゴリのみ表示される`() = runTest {
        categoriesFlow.value = listOf(
            makeCategory(id = 1L, ja = "表示中", en = "Visible", isHidden = false),
            makeCategory(id = 2L, ja = "非表示", en = "Hidden",  isHidden = true),
        )
        // DataStore経由でフィルターを DISABLED_ONLY に設定
        prefsFlow.value = UserPreferences(managementFilterMode = ManagementFilterMode.DISABLED_ONLY)

        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val names = viewModel.uiState.value.categories.map { it.nameEn }
        assertEquals(listOf("Hidden"), names)
        job.cancel()
    }

    @Test
    fun `ENABLED_ONLYフィルター - 表示中カテゴリのみ表示される`() = runTest {
        categoriesFlow.value = listOf(
            makeCategory(id = 1L, ja = "表示中", en = "Visible", isHidden = false),
            makeCategory(id = 2L, ja = "非表示", en = "Hidden",  isHidden = true),
        )
        prefsFlow.value = UserPreferences(managementFilterMode = ManagementFilterMode.ENABLED_ONLY)

        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val names = viewModel.uiState.value.categories.map { it.nameEn }
        assertEquals(listOf("Visible"), names)
        job.cancel()
    }

    // ----------------------------------------------------------------
    // isDragging
    // ----------------------------------------------------------------

    @Test
    fun `setIsDragging - trueを設定できる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setIsDragging(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDragging)
        job.cancel()
    }

    @Test
    fun `setIsDragging - falseに戻せる`() = runTest {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setIsDragging(true)
        advanceUntilIdle()
        viewModel.setIsDragging(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDragging)
        job.cancel()
    }

    // ----------------------------------------------------------------
    // ヘルパー
    // ----------------------------------------------------------------

    private fun makeCategory(
        id: Long,
        ja: String,
        en: String,
        isHidden: Boolean = false,
    ) = CategoryEntity(
        id        = id,
        nameJa    = ja,
        nameEn    = en,
        isHidden  = isHidden,
        parentId  = 1L,
    )

    private fun makeWord(
        id: Long,
        categoryId: Long,
        en: String,
        ja: String,
    ) = PromptWordEntity(
        id         = id,
        categoryId = categoryId,
        wordEn     = en,
        wordJa     = ja,
    )
}