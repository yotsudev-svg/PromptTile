package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Embedded

/**
 * 単語情報に所属カテゴリ名を付加した POJO。
 * フィルター表示（非表示のみ等）の際に文脈を補完するために使用する。
 */
data class PromptWordWithCategory(
    @Embedded
    val word: PromptWordEntity,
    val categoryNameJa: String,
    val categoryNameEn: String,
)
