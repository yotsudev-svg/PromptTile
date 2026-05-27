package com.blogspot.yotsudev.prompttile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blogspot.yotsudev.prompttile.data.dao.CategoryDao
import com.blogspot.yotsudev.prompttile.data.dao.PromptWordDao
import com.blogspot.yotsudev.prompttile.data.dao.SavedPromptDao
import com.blogspot.yotsudev.prompttile.data.dao.ToppingDao
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingGroupEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity

@Database(
    entities = [
        CategoryEntity::class,
        PromptWordEntity::class,
        SavedPromptEntity::class,
        ToppingGroupEntity::class,
        ToppingItemEntity::class,
    ],
    version = 9, // 8 → 9: SavedPromptEntityにsortOrder追加
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun promptWordDao(): PromptWordDao
    abstract fun savedPromptDao(): SavedPromptDao
    abstract fun toppingDao(): ToppingDao

    companion object {

        fun seedCallback(context: Context) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                val jsonText = context.assets
                    .open("seed_data.json")
                    .bufferedReader()
                    .use { it.readText() }

                // ---- カテゴリ＆単語のシード ----
                val seedData = parseSeedData(jsonText)
                seedData.categories.forEachIndexed { catIndex, category ->
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO categories
                            (id, nameJa, nameEn, sortOrder, isDefault, isHidden, isNegative)
                        VALUES (
                            ${category.id},
                            '${category.nameJa.escapeSql()}',
                            '${category.nameEn.escapeSql()}',
                            $catIndex,
                            1, 0,
                            ${if (category.isNegative) 1 else 0}
                        )
                        """.trimIndent()
                    )
                    category.words.forEachIndexed { wordIndex, word ->
                        val groupIdVal = if (word.toppingGroupId != null) word.toppingGroupId else "NULL"
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO prompt_words
                                (categoryId, wordEn, wordJa, sortOrder, isDefault, isHidden, toppingGroupId)
                            VALUES (
                                ${category.id},
                                '${word.wordEn.escapeSql()}',
                                '${word.wordJa.escapeSql()}',
                                $wordIndex,
                                1, 0,
                                $groupIdVal
                            )
                            """.trimIndent()
                        )
                    }
                }

                // ---- トッピンググループ＆アイテムのシード ----
                seedData.toppingGroups.forEachIndexed { _, group ->
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO topping_groups (id, nameJa, nameEn)
                        VALUES (${group.id}, '${group.nameJa.escapeSql()}', '${group.nameEn.escapeSql()}')
                        """.trimIndent()
                    )
                    group.items.forEachIndexed { itemIndex, item ->
                        val hexVal = if (item.colorHex != null) "'${item.colorHex}'" else "NULL"
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO topping_items
                                (groupId, valueEn, nameJa, colorHex, sortOrder)
                            VALUES (
                                ${group.id},
                                '${item.valueEn.escapeSql()}',
                                '${item.nameJa.escapeSql()}',
                                $hexVal,
                                $itemIndex
                            )
                            """.trimIndent()
                        )
                    }
                }

                // ---- テンプレートのシード (saved_prompts に統合) ----
                val templates = com.blogspot.yotsudev.prompttile.data.seed.parsePrefixTemplates(jsonText)
                templates.forEachIndexed { i, t ->
                    db.execSQL(
                        """
                        INSERT INTO saved_prompts (title, promptText, negativeText, isDefault, isEnabled, sortOrder, createdAt)
                        VALUES ('${t.name.escapeSql()}', '${t.text.escapeSql()}', '', 1, 1, $i, ${System.currentTimeMillis() + i})
                        """.trimIndent()
                    )
                }
            }
        }

        /** SQL インジェクション対策: シングルクォートをエスケープ */
        private fun String.escapeSql() = replace("'", "''")
    }
}