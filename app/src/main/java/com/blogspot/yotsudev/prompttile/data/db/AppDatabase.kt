package com.blogspot.yotsudev.prompttile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blogspot.yotsudev.prompttile.data.dao.CategoryDao
import com.blogspot.yotsudev.prompttile.data.dao.PromptWordDao
import com.blogspot.yotsudev.prompttile.data.dao.SavedPromptDao
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity

@Database(
    entities = [
        CategoryEntity::class,
        PromptWordEntity::class,
        SavedPromptEntity::class,
    ],
    version = 6, // 5 → 6: sortOrderの初期化と管理機能の追加
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun promptWordDao(): PromptWordDao
    abstract fun savedPromptDao(): SavedPromptDao

    companion object {
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 初期状態を ID 順に整える
                db.execSQL("UPDATE categories SET sortOrder = id")
                db.execSQL("UPDATE prompt_words SET sortOrder = id")
            }
        }

        fun seedCallback(context: Context) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                val json = context.assets
                    .open("seed_data.json")
                    .bufferedReader()
                    .use { it.readText() }

                val categories = parseSeedData(json)

                categories.forEachIndexed { catIndex, category ->
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO categories
                            (id, nameJa, nameEn, sortOrder, isDefault, isHidden, isNegative)
                        VALUES (
                            ${category.id},
                            '${category.nameJa}',
                            '${category.nameEn}',
                            $catIndex,
                            1,
                            0,
                            ${if (category.isNegative) 1 else 0}
                        )
                        """.trimIndent()
                    )

                    category.words.forEachIndexed { wordIndex, word ->
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO prompt_words
                                (categoryId, wordEn, wordJa, sortOrder, isDefault, isHidden)
                            VALUES (${category.id}, '${word.wordEn}', '${word.wordJa}', $wordIndex, 1, 0)
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }
}