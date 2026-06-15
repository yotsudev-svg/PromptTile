package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val isDefault: Boolean = false,
    val isHidden: Boolean = false,
    /**
     * 単語に付与されたタグ。カンマ区切り（例: "hair,style"）。
     * このタグに基づいて、TagRules からトッピンググループと除外設定を動的に解決する。
     */
    val tags: String? = null,
    /**
     * プロンプト生成用のテンプレート（例: "{colorA} to {colorB} gradient hair"）。
     * A/B のスロットに選択された色が埋め込まれる。
     */
    val promptTemplate: String? = null,
)
