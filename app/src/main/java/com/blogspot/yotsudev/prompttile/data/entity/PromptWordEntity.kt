package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 単語テーブル。カテゴリと1対多の関係を持つ。
 *
 * [ForeignKey] で参照整合性を保証し、親カテゴリ削除時に
 * CASCADE で子単語も自動削除されるようにする。
 *
 * [Index] を categoryId に張ることで、カテゴリ別の単語取得クエリが高速になる。
 */
@Entity(
    tableName = "prompt_words",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("categoryId")],
)
data class PromptWordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val wordEn: String,
    val wordJa: String,
    val sortOrder: Int = 0,
    /** trueのとき初期シードデータ。削除不可・非表示トグルのみ許可 */
    val isDefault: Boolean = false,
    /** trueのとき単語プールから非表示（シードデータのみ使用） */
    val isHidden: Boolean = false,
)