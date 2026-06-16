package com.blogspot.yotsudev.prompttile.data.repository

import com.blogspot.yotsudev.prompttile.data.dao.CategoryDao
import com.blogspot.yotsudev.prompttile.data.dao.HistoryDao
import com.blogspot.yotsudev.prompttile.data.dao.PromptWordDao
import com.blogspot.yotsudev.prompttile.data.dao.SavedPromptDao
import com.blogspot.yotsudev.prompttile.data.dao.ToppingDao
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.HistoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.ParentCategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordWithCategory
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingGroupEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity
import com.blogspot.yotsudev.prompttile.util.PromptFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

const val UNCATEGORIZED_POSITIVE_NAME = "Uncategorized"
const val UNCATEGORIZED_NEGATIVE_NAME = "Uncategorized (Negative)"

const val CATEGORY_ID_UNCATEGORIZED_POSITIVE = 998L
const val CATEGORY_ID_UNCATEGORIZED_NEGATIVE = 999L

@Singleton
class PromptRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val promptWordDao: PromptWordDao,
    private val savedPromptDao: SavedPromptDao,
    private val toppingDao: ToppingDao,
    private val historyDao: HistoryDao,
) {
    // ---- Parent Categories ----

    fun observeParentCategories(): Flow<List<ParentCategoryEntity>> =
        categoryDao.observeParentCategories().flowOn(Dispatchers.IO)

    fun observeParentCategories(isNegative: Boolean): Flow<List<ParentCategoryEntity>> =
        categoryDao.observeParentCategoriesByMode(isNegative).flowOn(Dispatchers.IO)

    // ---- Child Categories ----

    fun observeCategoriesByParent(parentId: Long, isNegative: Boolean): Flow<List<CategoryEntity>> =
        categoryDao.observeCategoriesByParent(parentId, isNegative, includeHidden = false).flowOn(Dispatchers.IO)

    val visibleCategories: Flow<List<CategoryEntity>> =
        categoryDao.observeCategories(isNegative = false)

    val visibleNegativeCategories: Flow<List<CategoryEntity>> =
        categoryDao.observeCategories(isNegative = true)

    fun visibleWordsByCategory(categoryId: Long): Flow<List<PromptWordEntity>> =
        promptWordDao.observeByCategory(categoryId, includeHidden = false).flowOn(Dispatchers.IO)

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.observeAll().flowOn(Dispatchers.IO)

    fun allWordsByCategory(categoryId: Long): Flow<List<PromptWordEntity>> =
        promptWordDao.observeByCategory(categoryId, includeHidden = true).flowOn(Dispatchers.IO)

    val allWordsWithCategory: Flow<List<PromptWordWithCategory>> =
        promptWordDao.observeAllWithCategory().flowOn(Dispatchers.IO)

    fun searchWords(query: String): Flow<List<PromptWordEntity>> =
        promptWordDao.searchWords(query).flowOn(Dispatchers.IO)

    val savedPrompts: Flow<List<SavedPromptEntity>> =
        savedPromptDao.observeAll().flowOn(Dispatchers.IO)

    // ---- トッピング取得 ----------------------------------------

    /**
     * 指定グループのトッピングアイテムを取得する（suspend版）。
     * ViewModel からワンショットで取得する際に使用する。
     */
    suspend fun getToppingItems(groupId: Long): List<ToppingItemEntity> =
        withContext(Dispatchers.IO) { toppingDao.getItemsByGroup(groupId) }

    suspend fun getToppingGroup(groupId: Long): ToppingGroupEntity? =
        withContext(Dispatchers.IO) { toppingDao.getGroupById(groupId) }

    /**
     * 指定グループのトッピングアイテムをリアクティブに監視（Flow版）。

     * 将来的にトッピング編集機能を追加した際に使用する想定。
     */
    fun observeToppingItems(groupId: Long): Flow<List<ToppingItemEntity>> =
        toppingDao.observeItemsByGroup(groupId).flowOn(Dispatchers.IO)

    // ---- カテゴリ CRUD ----------------------------------------

    suspend fun insertCategories(list: List<CategoryEntity>) =
        withContext(Dispatchers.IO) { categoryDao.insertAll(list) }

    suspend fun insertCategory(category: CategoryEntity) =
        withContext(Dispatchers.IO) { categoryDao.insert(category) }

    suspend fun updateCategory(category: CategoryEntity) =
        withContext(Dispatchers.IO) { categoryDao.update(category) }

    suspend fun updateCategories(categories: List<CategoryEntity>) =
        withContext(Dispatchers.IO) { categoryDao.updateAll(categories) }

    suspend fun resetCategoryOrder() =
        withContext(Dispatchers.IO) { categoryDao.resetOrderToId() }

    suspend fun deleteCategory(category: CategoryEntity) =
        withContext(Dispatchers.IO) { categoryDao.delete(category) }

    suspend fun toggleCategoryVisibility(category: CategoryEntity) =
        withContext(Dispatchers.IO) {
            categoryDao.update(category.copy(isHidden = !category.isHidden))
        }

    /** インポート時にカテゴリ名の重複チェックに使う（既存メソッドをラップ） */
    suspend fun getCategoryByNameEn(nameEn: String): CategoryEntity? =
        withContext(Dispatchers.IO) { categoryDao.getCategoryByNameEn(nameEn) }

    /** 新規カテゴリの sortOrder を末尾に設定するために現在の最大値を取得する */
    suspend fun getMaxCategorySortOrder(): Int =
        withContext(Dispatchers.IO) { categoryDao.getMaxSortOrder() ?: 0 }

    // ---- 単語 CRUD --------------------------------------------

    suspend fun insertWords(list: List<PromptWordEntity>) =
        withContext(Dispatchers.IO) { promptWordDao.insertAll(list) }

    suspend fun insertWord(word: PromptWordEntity) =
        withContext(Dispatchers.IO) { promptWordDao.insert(word) }

    suspend fun updateWord(word: PromptWordEntity) =
        withContext(Dispatchers.IO) { promptWordDao.update(word) }

    suspend fun updateWords(words: List<PromptWordEntity>) =
        withContext(Dispatchers.IO) { promptWordDao.updateAll(words) }

    suspend fun resetWordOrder(categoryId: Long) =
        withContext(Dispatchers.IO) { promptWordDao.resetOrderToId(categoryId) }

    suspend fun deleteWord(word: PromptWordEntity) =
        withContext(Dispatchers.IO) { promptWordDao.delete(word) }

    suspend fun toggleWordVisibility(word: PromptWordEntity) =
        withContext(Dispatchers.IO) {
            promptWordDao.update(word.copy(isHidden = !word.isHidden))
        }

    suspend fun moveWordToCategory(wordId: Long, newCategoryId: Long) =
        withContext(Dispatchers.IO) { promptWordDao.updateCategory(wordId, newCategoryId) }

    suspend fun registerNewWordsFromText(text: String, isNegative: Boolean) =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext
            val catId = if (isNegative) CATEGORY_ID_UNCATEGORIZED_NEGATIVE else CATEGORY_ID_UNCATEGORIZED_POSITIVE
            val existing = promptWordDao.getWordEnsByCategory(catId).toHashSet()
            val newWords = text.split(",")
                .map { PromptFormatter.cleanWord(it.trim()) }
                .filter { it.isNotBlank() && it !in existing }
                .distinct()
                .mapIndexed { i, w ->
                    PromptWordEntity(categoryId = catId, wordEn = w, wordJa = "", sortOrder = i)
                }
            if (newWords.isNotEmpty()) promptWordDao.insertAll(newWords)
        }

    suspend fun getWordEnsByCategory(categoryId: Long): List<String> =
        withContext(Dispatchers.IO) { promptWordDao.getWordEnsByCategory(categoryId) }

    /**
     * インポート用データ（ImportCategoryのリスト）をDBにマージ保存する。
     * 同名カテゴリがあれば再利用し、重複しない単語のみを追加する。
     * @return Pair(追加されたカテゴリ数, 追加された単語数)
     */
    suspend fun importCategories(categories: List<com.blogspot.yotsudev.prompttile.data.importer.ImportCategory>): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            var addedCategories = 0
            var addedWords = 0

            categories.forEach { importCat ->
                // 同名カテゴリが既にあれば再利用、なければ新規作成
                val existing = categoryDao.getCategoryByNameEn(importCat.nameEn)
                val categoryId = if (existing != null) {
                    existing.id
                } else {
                    val maxOrder = categoryDao.getMaxSortOrder() ?: 0
                    val newCat = CategoryEntity(
                        nameJa = importCat.nameJa,
                        nameEn = importCat.nameEn,
                        parentId = importCat.parentId,
                        isNegative = importCat.isNegative,
                        sortOrder = maxOrder + 1,
                    )
                    addedCategories++
                    categoryDao.insert(newCat)
                }

                // 既存単語と重複しないものだけ挿入
                val existingWords = promptWordDao.getWordEnsByCategory(categoryId).toHashSet()
                val newWords = importCat.words
                    .filter { it.wordEn !in existingWords }
                    .mapIndexed { i, w ->
                        PromptWordEntity(
                            categoryId = categoryId,
                            wordEn = w.wordEn,
                            wordJa = w.wordJa,
                            sortOrder = i,
                        )
                    }

                if (newWords.isNotEmpty()) {
                    promptWordDao.insertAll(newWords)
                    addedWords += newWords.size
                }
            }
            Pair(addedCategories, addedWords)
        }

    // ---- 保存済みプロンプト ------------------------------------

    suspend fun savePrompt(entity: SavedPromptEntity) =
        withContext(Dispatchers.IO) { savedPromptDao.insert(entity) }

    suspend fun updatePrompt(entity: SavedPromptEntity) =
        withContext(Dispatchers.IO) { savedPromptDao.update(entity) }

    suspend fun deletePrompt(entity: SavedPromptEntity) =
        withContext(Dispatchers.IO) { savedPromptDao.delete(entity) }

    suspend fun updateSavedPrompts(list: List<SavedPromptEntity>) =
        withContext(Dispatchers.IO) { savedPromptDao.updateAll(list) }

    suspend fun resetSavedPromptOrder() =
        withContext(Dispatchers.IO) { savedPromptDao.resetOrderToId() }

    // ---- コピー履歴 --------------------------------------------

    val copyHistories: Flow<List<HistoryEntity>> = historyDao.observeAll().flowOn(Dispatchers.IO)

    suspend fun saveHistory(positive: String, negative: String, limit: Int) = withContext(Dispatchers.IO) {
        if (positive.isBlank() && negative.isBlank()) return@withContext
        
        // 重複チェック (最新のものと同じならスキップ)
        val latest = historyDao.observeAll().first().firstOrNull()
        if (latest?.positivePrompt == positive && latest.negativePrompt == negative) return@withContext

        historyDao.insert(HistoryEntity(positivePrompt = positive, negativePrompt = negative))
        
        // 件数制御 (無制限=0 または 負の値 の場合はスキップ)
        if (limit > 0) {
            val count = historyDao.getCount()
            if (count > limit) {
                historyDao.deleteOldest(count - limit)
            }
        }
    }

    suspend fun deleteHistory(entity: HistoryEntity) = withContext(Dispatchers.IO) {
        historyDao.delete(entity)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        historyDao.deleteAll()
    }
}
