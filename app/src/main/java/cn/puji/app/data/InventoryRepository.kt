package cn.puji.app.data

import android.content.ContentValues
import cn.puji.app.domain.Cart
import cn.puji.app.domain.DateRange
import cn.puji.app.domain.DocumentLine
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.InventoryService
import cn.puji.app.domain.StockDocument
import cn.puji.app.domain.StockMath

class InventoryRepository(
    private val database: PujiDatabase,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    fun commit(cart: Cart): Result<Long> {
        InventoryService.validate(cart).getOrElse { return Result.failure(it) }
        return runCatching {
            val db = database.writableDatabase
            db.beginTransaction()
            try {
                val now = nowProvider()
                val documentId = db.insertOrThrow("documents", null, ContentValues().apply {
                    put("type", cart.type.name)
                    put("total_cents", cart.totalCents)
                    put("note", cart.note.trim())
                    put("created_at", now)
                })
                cart.lines.forEach { line ->
                    val current = db.rawQuery(
                        "SELECT stock_quantity FROM products WHERE id = ? AND active = 1",
                        arrayOf(line.product.id.toString()),
                    ).use { cursor -> require(cursor.moveToFirst()) { "商品不存在或已停用" }; cursor.getInt(0) }
                    val next = StockMath.nextStock(cart.type, current, line.quantity).getOrThrow()
                    val changed = db.update("products", ContentValues().apply {
                        put("stock_quantity", next)
                        put("updated_at", now)
                    }, "id = ? AND stock_quantity = ?", arrayOf(line.product.id.toString(), current.toString()))
                    check(changed == 1) { "库存已变化，请重试" }
                    db.insertOrThrow("document_lines", null, ContentValues().apply {
                        put("document_id", documentId)
                        put("product_id", line.product.id)
                        put("product_name_snapshot", line.product.name)
                        put("barcode_snapshot", line.product.barcode)
                        put("unit_price_cents", line.unitPriceCents)
                        put(
                            "cost_price_snapshot_cents",
                            if (cart.type == DocumentType.INBOUND) line.unitPriceCents else line.product.costPriceCents,
                        )
                        put("quantity", line.quantity)
                        put("line_total_cents", line.totalCents)
                    })
                    db.insertOrThrow("stock_movements", null, ContentValues().apply {
                        put("product_id", line.product.id)
                        put("document_id", documentId)
                        put("type", cart.type.name)
                        put("quantity_delta", if (cart.type == DocumentType.INBOUND) line.quantity else -line.quantity)
                        put("stock_after", next)
                        put("created_at", now)
                    })
                }
                db.setTransactionSuccessful()
                documentId
            } finally {
                db.endTransaction()
            }
        }
    }

    fun documents(type: DocumentType? = null, range: DateRange? = null): List<StockDocument> {
        val clauses = mutableListOf<String>()
        val values = mutableListOf<String>()
        type?.let {
            clauses += "type = ?"
            values += it.name
        }
        range?.let {
            clauses += "created_at >= ?"
            values += it.startInclusive.toString()
            clauses += "created_at < ?"
            values += it.endExclusive.toString()
        }
        val where = clauses.joinToString(" AND ").ifEmpty { null }
        val args = values.toTypedArray().takeIf { it.isNotEmpty() }
        return database.readableDatabase.query(
            "documents", null, where, args, null, null, "created_at DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    add(
                        StockDocument(
                            id = id,
                            type = DocumentType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type"))),
                            totalCents = cursor.getLong(cursor.getColumnIndexOrThrow("total_cents")),
                            note = cursor.getString(cursor.getColumnIndexOrThrow("note")),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                            lines = lines(id),
                        ),
                    )
                }
            }
        }
    }

    private fun lines(documentId: Long): List<DocumentLine> = database.readableDatabase.query(
        "document_lines", null, "document_id = ?", arrayOf(documentId.toString()), null, null, "id",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(
                DocumentLine(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    documentId = documentId,
                    productId = cursor.getLong(cursor.getColumnIndexOrThrow("product_id")),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name_snapshot")),
                    barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode_snapshot")),
                    unitPriceCents = cursor.getLong(cursor.getColumnIndexOrThrow("unit_price_cents")),
                    costPriceSnapshotCents = cursor.getLong(cursor.getColumnIndexOrThrow("cost_price_snapshot_cents")),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                ),
            )
        }
    }
}
