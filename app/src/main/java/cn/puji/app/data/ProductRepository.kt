package cn.puji.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import cn.puji.app.domain.Product

class DuplicateBarcodeException : IllegalArgumentException("这个条码已经属于其他商品")

class ProductRepository(private val database: PujiDatabase) {
    fun list(query: String = "", includeInactive: Boolean = false): List<Product> {
        val normalized = query.trim()
        val where = buildList {
            if (!includeInactive) add("active = 1")
            if (normalized.isNotEmpty()) add("(name LIKE ? OR barcode LIKE ? OR category LIKE ?)")
        }.joinToString(" AND ").ifEmpty { null }
        val args = if (normalized.isNotEmpty()) Array(3) { "%$normalized%" } else null
        return database.readableDatabase.query(
            "products", null, where, args, null, null,
            "CASE WHEN stock_quantity <= low_stock_threshold THEN 0 ELSE 1 END, updated_at DESC",
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toProduct()) } }
    }

    fun find(id: Long): Product? = database.readableDatabase.query(
        "products", null, "id = ?", arrayOf(id.toString()), null, null, null, "1",
    ).use { if (it.moveToFirst()) it.toProduct() else null }

    fun findByBarcode(raw: String): Product? {
        val barcode = raw.trim()
        if (barcode.isEmpty()) return null
        return database.readableDatabase.query(
            "products", null, "barcode = ? AND active = 1", arrayOf(barcode), null, null, null, "1",
        ).use { if (it.moveToFirst()) it.toProduct() else null }
    }

    fun save(product: Product): Result<Long> = runCatching {
        require(product.name.isNotBlank()) { "请输入商品名称" }
        require(product.costPriceCents >= 0 && product.salePriceCents >= 0) { "价格不能为负数" }
        require(product.stockQuantity >= 0 && product.lowStockThreshold >= 0) { "库存不能为负数" }
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("name", product.name.trim())
            if (product.barcode.isBlank()) putNull("barcode") else put("barcode", product.barcode.trim())
            put("category", product.category.trim().ifBlank { "未分类" })
            put("cost_price_cents", product.costPriceCents)
            put("sale_price_cents", product.salePriceCents)
            put("stock_quantity", product.stockQuantity)
            put("low_stock_threshold", product.lowStockThreshold)
            put("note", product.note.trim())
            put("active", if (product.active) 1 else 0)
            put("updated_at", now)
            if (product.id == 0L) put("created_at", now)
        }
        try {
            if (product.id == 0L) database.writableDatabase.insertOrThrow("products", null, values)
            else {
                val changed = database.writableDatabase.update("products", values, "id = ?", arrayOf(product.id.toString()))
                require(changed == 1) { "商品不存在" }
                product.id
            }
        } catch (_: SQLiteConstraintException) {
            throw DuplicateBarcodeException()
        }
    }

    fun deactivate(id: Long): Result<Unit> = runCatching {
        val values = ContentValues().apply { put("active", 0); put("updated_at", System.currentTimeMillis()) }
        require(database.writableDatabase.update("products", values, "id = ?", arrayOf(id.toString())) == 1)
    }
}

internal fun Cursor.toProduct() = Product(
    id = getLong(getColumnIndexOrThrow("id")),
    name = getString(getColumnIndexOrThrow("name")),
    barcode = getString(getColumnIndexOrThrow("barcode")) ?: "",
    category = getString(getColumnIndexOrThrow("category")),
    costPriceCents = getLong(getColumnIndexOrThrow("cost_price_cents")),
    salePriceCents = getLong(getColumnIndexOrThrow("sale_price_cents")),
    stockQuantity = getInt(getColumnIndexOrThrow("stock_quantity")),
    lowStockThreshold = getInt(getColumnIndexOrThrow("low_stock_threshold")),
    note = getString(getColumnIndexOrThrow("note")),
    active = getInt(getColumnIndexOrThrow("active")) == 1,
    createdAt = getLong(getColumnIndexOrThrow("created_at")),
    updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
)

