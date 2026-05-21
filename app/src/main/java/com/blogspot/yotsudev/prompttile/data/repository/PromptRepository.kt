package com.blogspot.yotsudev.prompttile.data.repository

import com.blogspot.yotsudev.prompttile.data.dao.CategoryDao
import com.blogspot.yotsudev.prompttile.data.dao.PromptWordDao
import com.blogspot.yotsudev.prompttile.data.dao.SavedPromptDao
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import kotlinx.coroutines.flow.Flow
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
        promptWordDao.observeByCategory(categoryId, includeHidden = false)

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun allWordsByCategory(categoryId: Long): Flow<List<PromptWordEntity>> =
        promptWordDao.observeByCategory(categoryId, includeHidden = true)

    val savedPrompts: Flow<List<SavedPromptEntity>> = savedPromptDao.observeAll()

    suspend fun insertCategories(list: List<CategoryEntity>) = categoryDao.insertAll(list)
    suspend fun insertWords(list: List<PromptWordEntity>) = promptWordDao.insertAll(list)
    suspend fun insertCategory(category: CategoryEntity) = categoryDao.insert(category)
    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)
    suspend fun toggleCategoryVisibility(category: CategoryEntity) =
        categoryDao.update(category.copy(isHidden = !category.isHidden))

    suspend fun insertWord(word: PromptWordEntity) = promptWordDao.insert(word)
    suspend fun updateWord(word: PromptWordEntity) = promptWordDao.update(word)
    suspend fun deleteWord(word: PromptWordEntity) = promptWordDao.delete(word)
    suspend fun toggleWordVisibility(word: PromptWordEntity) =
        promptWordDao.update(word.copy(isHidden = !word.isHidden))

    suspend fun moveWordToCategory(wordId: Long, newCategoryId: Long) =
        promptWordDao.updateCategory(wordId, newCategoryId)

    /**
     * テキストに含まれる単語をDBに一括登録する。
     *
     * 重複チェックを getWordEnsByCategory でカテゴリ単位に限定することで、
     * ポジとネガに同じ単語が存在することを許容する。
     * （例: ポジの未分類に "blurry" があってもネガの未分類にも登録できる）
     */
    suspend fun registerNewWordsFromText(text: String, isNegative: Boolean) {
        if (text.isBlank()) return

        val uncategorizedName = if (isNegative)
            UNCATEGORIZED_NEGATIVE_NAME else UNCATEGORIZED_POSITIVE_NAME
        val uncategorizedCategory = categoryDao.getCategoryByNameEn(uncategorizedName) ?: return

        // 全単語ではなく対象カテゴリ内のみで重複チェック
        val existingWords = promptWordDao
            .getWordEnsByCategory(uncategorizedCategory.id)
            .toHashSet()

        val newWords = text
            .split(",")
            .map { it.trim().cleanWord() }
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

    suspend fun savePrompt(entity: SavedPromptEntity) = savedPromptDao.insert(entity)
    suspend fun deletePrompt(entity: SavedPromptEntity) = savedPromptDao.delete(entity)
}

private fun String.cleanWord(): String =
    this.replace(Regex("[()\\[\\]{}]"), "")
        .split(":")[0]
        .trim()