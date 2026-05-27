package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 保存済みプロンプトテーブル。
 *
 * [negativeText] を追加してポジ・ネガをセットで保存できるようにした。
 * 既存レコードは negativeText が空文字として扱われる。
 * fallbackToDestructiveMigration() 使用中のため、
 * version を 3 に上げるだけでDBが再作成される。
 */
@Entity(tableName = "saved_prompts")
data class SavedPromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val promptText: String,
    val negativeText: String = "",
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)