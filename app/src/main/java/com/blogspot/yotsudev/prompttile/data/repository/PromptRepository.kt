package com.blogspot.yotsudev.prompttile.data.repository

import com.blogspot.yotsudev.prompttile.data.dao.CategoryDao
import com.blogspot.yotsudev.prompttile.data.dao.PromptWordDao
import com.blogspot.yotsudev.prompttile.data.dao.SavedPromptDao
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.util.PromptFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

const val UNCATEGORIZED_POSITIVE_NAME = "Uncategorized"
const val UNCATEGORIZED_NEGATIVE_NAME = "Uncategorized (Negative)"

@Singleton
class PromptRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val promptWordDao: PromptWordDao,
    private val savedPromptDao: SavedPromptDao,
) {
    // カテゴリの取得（引数でポジティブ/ネガティブを分ける）
    val visibleCategories: Flow<List<CategoryEntity>> = 
        categoryDao.observeCategories(isNegative = false)
    
    val visibleNegativeCategories: Flow<List<CategoryEntity>> = 
        categoryDao.observeCategories(isNegative = true)

    // 単語の取得（引数で非表示を含めるかどうかを制御）
    fun visibleWordsByCategory(categoryId: Long): Flow<List<PromptWordEntity>> =
        promptWordDao.observeByCategory(categoryId, includeHidden = false).flowOn(Dispatchers.IO)

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.observeAll().flowOn(Dispatchers.IO)

    fun allWordsByCategory(categoryId: Long): Flow<List<PromptWordEntity>> =
        promptWordDao.observeByCategory(categoryId, includeHidden = true).flowOn(Dispatchers.IO)

    fun searchWords(query: String): Flow<List<PromptWordEntity>> =
        promptWordDao.searchWords(query).flowOn(Dispatchers.IO)


    val savedPrompts: Flow<List<SavedPromptEntity>> = savedPromptDao.observeAll().flowOn(Dispatchers.IO)

    suspend fun insertCategories(list: List<CategoryEntity>) = withContext(Dispatchers.IO) { categoryDao.insertAll(list) }
    suspend fun insertWords(list: List<PromptWordEntity>) = withContext(Dispatchers.IO) { promptWordDao.insertAll(list) }
    suspend fun insertCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.insert(category) }
    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.update(category) }
    suspend fun updateCategories(categories: List<CategoryEntity>) = withContext(Dispatchers.IO) {
        categoryDao.updateAll(categories)
    }
    suspend fun resetCategoryOrder() = withContext(Dispatchers.IO) {
        categoryDao.resetOrderToId()
    }
    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.delete(category) }
    suspend fun toggleCategoryVisibility(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.update(category.copy(isHidden = !category.isHidden))
    }

    suspend fun insertWord(word: PromptWordEntity) = withContext(Dispatchers.IO) { promptWordDao.insert(word) }
    suspend fun updateWord(word: PromptWordEntity) = withContext(Dispatchers.IO) { promptWordDao.update(word) }
    suspend fun updateWords(words: List<PromptWordEntity>) = withContext(Dispatchers.IO) {
        promptWordDao.updateAll(words)
    }
    suspend fun resetWordOrder(categoryId: Long) = withContext(Dispatchers.IO) {
        promptWordDao.resetOrderToId(categoryId)
    }
    suspend fun deleteWord(word: PromptWordEntity) = withContext(Dispatchers.IO) { promptWordDao.delete(word) }
    suspend fun toggleWordVisibility(word: PromptWordEntity) = withContext(Dispatchers.IO) {
        promptWordDao.update(word.copy(isHidden = !word.isHidden))
    }

    suspend fun moveWordToCategory(wordId: Long, newCategoryId: Long) = withContext(Dispatchers.IO) {
        promptWordDao.updateCategory(wordId, newCategoryId)
    }

    /**
     * テキストに含まれる単語をDBに一括登録する。
     *
     * 重複チェックを getWordEnsByCategory でカテゴリ単位に限定することで、
     * ポジとネガに同じ単語が存在することを許容する。
     * （例: ポジの未分類に "blurry" があってもネガの未分類にも登録できる）
     */
    suspend fun registerNewWordsFromText(text: String, isNegative: Boolean) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext

        val uncategorizedName = if (isNegative)
            UNCATEGORIZED_NEGATIVE_NAME else UNCATEGORIZED_POSITIVE_NAME
        val uncategorizedCategory = categoryDao.getCategoryByNameEn(uncategorizedName) ?: return@withContext

        // 全単語ではなく対象カテゴリ内のみで重複チェック
        val existingWords = promptWordDao
            .getWordEnsByCategory(uncategorizedCategory.id)
            .toHashSet()

        val newWords = text
            .split(",")
            .map { it.trim().let { w -> PromptFormatter.cleanWord(w) } }
            .filter { it.isNotBlank() && it !in existingWords }
            .distinct()
            .mapIndexed { index, word ->
                PromptWordEntity(
                    categoryId = uncategorizedCategory.id,
                    wordEn     = word,
                    wordJa     = "",
                    sortOrder  = index,
                    isDefault  = false,
                )
            }

        if (newWords.isNotEmpty()) {
            promptWordDao.insertAll(newWords)
        }
    }

    suspend fun savePrompt(entity: SavedPromptEntity) = withContext(Dispatchers.IO) { savedPromptDao.insert(entity) }
    suspend fun deletePrompt(entity: SavedPromptEntity) = withContext(Dispatchers.IO) { savedPromptDao.delete(entity) }
}