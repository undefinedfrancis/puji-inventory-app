package cn.puji.app.data

import android.content.Context
import cn.puji.app.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedDraft(val cart: Cart, val requestId: String)

class DraftRepository(context: Context) {
    private val preferences = context.getSharedPreferences("puji_draft", Context.MODE_PRIVATE)

    fun load(): SavedDraft? {
        if(preferences.getBoolean("replacing", false)) { clear(); return null }
        val raw = preferences.getString("draft", null) ?: return null
        require(raw.length <= 1_048_576) { "草稿过大" }
        val json = JSONObject(raw)
        val type = DocumentType.valueOf(json.getString("type"))
        require(type == DocumentType.INBOUND || type == DocumentType.OUTBOUND)
        val array = json.getJSONArray("lines")
        require(array.length() <= 500)
        val lines = (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            val p = Product(id = row.getLong("id"), name = row.getString("name"), barcode = row.getString("barcode"),
                category = row.getString("category"), costPriceCents = row.getLong("cost"), salePriceCents = row.getLong("sale"),
                stockQuantity = row.getInt("stock"), lowStockThreshold = row.optInt("threshold", 5), note = row.optString("productNote"), uuid = row.optString("uuid"))
            require(p.id <= 0 || p.uuid.isNotBlank()) { "旧清单的商品标识已失效" }
            CartLine(p, row.getInt("quantity"), row.getLong("price"))
        }
        val cart = Cart(type, lines, json.optString("note"))
        InventoryService.validate(cart).getOrThrow()
        return SavedDraft(cart, json.getString("requestId").also { UUID.fromString(it) })
    }

    fun save(cart: Cart, requestId: String, durable: Boolean = false) {
        if (cart.lines.isEmpty()) { clear(); return }
        val lines = JSONArray()
        cart.lines.forEach { line ->
            val p = line.product
            lines.put(JSONObject().put("id", p.id).put("name", p.name).put("barcode", p.barcode).put("category", p.category)
                .put("cost", p.costPriceCents).put("sale", p.salePriceCents).put("stock", p.stockQuantity)
                .put("threshold", p.lowStockThreshold).put("productNote", p.note).put("uuid", p.uuid)
                .put("quantity", line.quantity).put("price", line.unitPriceCents))
        }
        val editor = preferences.edit().putString("draft", JSONObject().put("type", cart.type.name).put("note", cart.note)
            .put("requestId", requestId).put("lines", lines).toString())
        if (durable) check(editor.commit()) { "清单保存失败，请重试" } else editor.apply()
    }

    fun beginReplacement() { check(preferences.edit().putBoolean("replacing", true).commit()) { "无法安全暂停当前清单" } }
    fun cancelReplacement() { check(preferences.edit().remove("replacing").commit()) { "无法恢复清单状态" } }
    fun clear() { check(preferences.edit().remove("draft").remove("replacing").commit()) { "无法清除草稿" } }
}
