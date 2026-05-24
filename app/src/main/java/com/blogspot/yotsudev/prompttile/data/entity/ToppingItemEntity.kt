package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * トッピングアイテムテーブル。
 * グループに属する個々の選択肢（例: "red", "blue"）を管理する。
 *
 * [colorHex]: "#FF0000" 形式のカラーコード。
 *   色がない素材系アイテム（例: "leather"）は null を許容する。
 *   UIではこの値の有無でカラードットの表示を切り替える。
 */
@Entity(
    tableName = "topping_items",
    foreignKeys = [
        ForeignKey(
            entity = ToppingGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("groupId")],
)
data class ToppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    /** プロンプトに挿入される英語文字列（例: "red", "leather"）*/
    val valueEn: String,
    /** UIに表示する日本語ラベル（例: "赤", "レザー"）*/
    val nameJa: String,
    /** カラードット表示用HEX文字列。色がない場合は null */
    val colorHex: String? = null,
    val sortOrder: Int = 0,
)