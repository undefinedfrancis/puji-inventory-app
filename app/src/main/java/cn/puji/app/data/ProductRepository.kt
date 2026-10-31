package cn.puji.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import cn.puji.app.domain.Product

class DuplicateBarcodeException : IllegalArgumentException("这个条码已经属于其他商品")

class ProductRepository(private val database: PujiDatabase) {
    fun list(
        query: String = "",
        category: String? = null,
        includeInactive: Boolean = false,
    ): List<Product> {
        val normalized = query.trim()
        val where = buildList {
            if (!includeInactive) add("active = 1")
            if (normalized.isNotEmpty()) add("(name LIKE ? OR barcode LIKE ? OR category LIKE ?)")
            if (category != null && category != "全部") {
                add("COALESCE(NULLIF(TRIM(category), ''), '未分类') = ?")
            }
        }.joinToString(" AND ").ifEmpty { null }
        val argumentList = buildList {
            if (normalized.isNotEmpty()) repeat(3) { add("%$normalized%") }
            if (category != null && category != "全部") add(category.trim().ifBlank { "未分类" })
        }
        val args = argumentList.toTypedArray().takeIf { it.isNotEmpty() }
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
        val db = database.writableDatabase
        ledgerTransaction(db) { saveInTransaction(db, product) }
    }

    /** Barcode matching and every opening entry commit together, including inactive products. */
    fun importProducts(products: List<Product>): Result<List<Long>> = runCatching {
        val db = database.writableDatabase
        ledgerTransaction(db) {
            products.map { product ->
                val existing = if (product.barcode.isBlank()) null else db.query(
                    "products", null, "barcode = ?", arrayOf(product.barcode.trim()), null, null, null,
                ).use { if (it.moveToFirst()) it.toProduct() else null }
                saveInTransaction(db, if (existing != null) product.copy(id = existing.id, active = existing.active) else product.copy(id = 0))
            }
        }
    }

    private fun saveInTransaction(db: android.database.sqlite.SQLiteDatabase, product: Product): Long {
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
            put("low_stock_threshold", product.lowStockThreshold)
            put("note", product.note.trim())
            put("active", if (product.active) 1 else 0)
            put("updated_at", now)
            if (product.id == 0L) {
                put("created_at", now)
                put("uuid", java.util.UUID.randomUUID().toString())
                put("stock_quantity", 0)
                put("stock_cost_cents", 0)
            }
        }
        val id = try {
            if (product.id == 0L) db.insertOrThrow("products", null, values)
            else {
                val version = db.rawQuery("SELECT version FROM products WHERE id=?", arrayOf(product.id.toString())).use {
                    require(it.moveToFirst()) { "商品不存在" }; it.getLong(0)
                }
                values.put("version", Math.addExact(version,1))
                require(db.update("products", values, "id = ?", arrayOf(product.id.toString())) == 1) { "商品不存在" }
                product.id
            }
        } catch (_: SQLiteConstraintException) { throw DuplicateBarcodeException() }
        if (product.id == 0L && product.stockQuantity > 0) {
            InventoryRepository(database).opening(db, id, product.stockQuantity, product.costPriceCents, now)
        }
        return id
    }

    fun activate(id: Long): Result<Unit> = setActive(id, true)
    fun reactivate(id: Long): Result<Unit> = setActive(id, true)
    fun deactivate(id: Long): Result<Unit> = setActive(id, false)
    fun setActive(id: Long, active: Boolean): Result<Unit> = runCatching {
        val db=database.writableDatabase
        ledgerTransaction(db) {
            val version=db.rawQuery("SELECT version FROM products WHERE id=?",arrayOf(id.toString())).use {
                require(it.moveToFirst()) { "商品不存在" };it.getLong(0)
            }
            val values=ContentValues().apply {
                put("active",if(active) 1 else 0);put("updated_at",System.currentTimeMillis());put("version",Math.addExact(version,1))
            }
            require(db.update("products",values,"id=?",arrayOf(id.toString()))==1) { "商品不存在" }
        }
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
    uuid = getString(getColumnIndexOrThrow("uuid")),
    stockCostCents = getLong(getColumnIndexOrThrow("stock_cost_cents")),
    version = getLong(getColumnIndexOrThrow("version")),
    source = getString(getColumnIndexOrThrow("source")),
    costSource = getString(getColumnIndexOrThrow("cost_source")),
)
