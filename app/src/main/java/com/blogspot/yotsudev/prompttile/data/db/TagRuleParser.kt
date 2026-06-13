package com.blogspot.yotsudev.prompttile.data.db

import com.blogspot.yotsudev.prompttile.data.entity.TagRule
import org.json.JSONObject

/**
 * TagRules JSON をパースする。
 */
fun parseTagRules(json: String): List<TagRule> {
    val root = JSONObject(json)
    val rulesArray = root.getJSONArray("tag_rules")
    
    return List(rulesArray.length()) { i ->
        val obj = rulesArray.getJSONObject(i)
        TagRule(
            tag = obj.getString("tag"),
            toppingGroupIds = if (obj.has("toppingGroupIds")) {
                val arr = obj.getJSONArray("toppingGroupIds")
                List(arr.length()) { idx -> arr.getLong(idx) }
            } else emptyList(),
            excludeToppingValues = if (obj.has("excludeToppingValues")) {
                val arr = obj.getJSONArray("excludeToppingValues")
                List(arr.length()) { idx -> arr.getString(idx) }
            } else emptyList()
        )
    }
}
