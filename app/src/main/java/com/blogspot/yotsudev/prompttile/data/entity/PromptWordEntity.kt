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
     * 紐づくトッピンググループのID。null の場合はトッピング非対応の通常単語。
     *
     * ToppingGroupEntity への外部キーは張らない（Step1参照）。
     * WordPool での表示分岐キーとして使用する:
     *   null     → 通常チップ（従来デザイン）
     *   non-null → 分割スマート・アシストチップ（🎨アイコン付き）
     */
    val toppingGroupId: Long? = null,
)