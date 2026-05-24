package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * トッピンググループテーブル。
 * 「よく使う色」「髪色セット」など、単語に紐づける選択肢のまとまりを管理する。
 *
 * 1つのグループを複数の単語から参照できる（多対多の参照）ため、
 * PromptWordEntity 側に groupId を持たせる形（外部キーなし）で実装する。
 * 外部キーを張らない理由: グループ削除時にすべての紐づき単語の
 * toppingGroupId を NULL に更新するよりも、アプリ側で制御する方が
 * 柔軟性が高いため。
 */
@Entity(tableName = "topping_groups")
data class ToppingGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nameJa: String,
    val nameEn: String,
)