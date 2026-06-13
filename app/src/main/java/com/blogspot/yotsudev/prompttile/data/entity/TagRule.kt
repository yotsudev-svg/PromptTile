package com.blogspot.yotsudev.prompttile.data.entity

/**
 * タグに基づくトッピングの適用ルール。
 *
 * @param tag 対象のタグ名（例: "hair", "eye"）
 * @param toppingGroupIds このタグを持つ単語に適用するトッピンググループのIDリスト
 * @param excludeToppingValues このタグを持つ場合に除外するトッピングアイテムの valueEn リスト
 */
data class TagRule(
    val tag: String,
    val toppingGroupIds: List<Long>,
    val excludeToppingValues: List<String> = emptyList()
)
