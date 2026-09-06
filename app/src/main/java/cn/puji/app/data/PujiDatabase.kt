package cn.puji.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PujiDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                barcode TEXT,
                category TEXT NOT NULL DEFAULT '未分类',
                cost_price_cents INTEGER NOT NULL DEFAULT 0,
                sale_price_cents INTEGER NOT NULL DEFAULT 0,
                stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK(stock_quantity >= 0),
                low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK(low_stock_threshold >= 0),
                note TEXT NOT NULL DEFAULT '',
                active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL AND barcode <> ''")
        db.execSQL("CREATE INDEX idx_products_name ON products(name)")
        db.execSQL(
            """CREATE TABLE documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL CHECK(type IN ('INBOUND','OUTBOUND')),
                total_cents INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE document_lines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                document_id INTEGER NOT NULL REFERENCES documents(id),
                product_id INTEGER NOT NULL REFERENCES products(id),
                product_name_snapshot TEXT NOT NULL,
                barcode_snapshot TEXT NOT NULL DEFAULT '',
                unit_price_cents INTEGER NOT NULL,
                cost_price_snapshot_cents INTEGER NOT NULL DEFAULT 0,
                quantity INTEGER NOT NULL CHECK(quantity > 0),
                line_total_cents INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE stock_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL REFERENCES products(id),
                document_id INTEGER NOT NULL REFERENCES documents(id),
                type TEXT NOT NULL,
                quantity_delta INTEGER NOT NULL,
                stock_after INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_documents_created ON documents(created_at DESC)")
        db.execSQL("CREATE INDEX idx_documents_type_created ON documents(type, created_at DESC)")
        db.execSQL("CREATE INDEX idx_lines_document ON document_lines(document_id)")
        seedProducts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE document_lines ADD COLUMN cost_price_snapshot_cents INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                """UPDATE document_lines
                   SET cost_price_snapshot_cents = CASE
                       WHEN (SELECT d.type FROM documents d WHERE d.id = document_lines.document_id) = 'INBOUND'
                           THEN unit_price_cents
                       ELSE COALESCE(
                           (SELECT p.cost_price_cents FROM products p WHERE p.id = document_lines.product_id),
                           0
                       )
                   END""".trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_type_created ON documents(type, created_at DESC)")
        }
    }

    private fun seedProducts(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT INTO products(name,barcode,category,cost_price_cents,sale_price_cents,stock_quantity,low_stock_threshold,note,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
            arrayOf<Any>("可口可乐 500ml", "6901234567890", "饮料", 220, 350, 18, 5, "示例商品，可编辑", now, now),
        )
        db.execSQL(
            "INSERT INTO products(name,barcode,category,cost_price_cents,sale_price_cents,stock_quantity,low_stock_threshold,note,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
            arrayOf<Any>("农夫山泉 550ml", "6901234567891", "饮料", 100, 200, 4, 6, "低库存示例", now, now),
        )
        db.execSQL(
            "INSERT INTO products(name,barcode,category,cost_price_cents,sale_price_cents,stock_quantity,low_stock_threshold,note,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
            arrayOf<Any>("康师傅红烧牛肉面", "6901234567892", "食品", 320, 450, 24, 8, "", now, now),
        )
    }

    companion object {
        private const val DATABASE_NAME = "puji.db"
        private const val DATABASE_VERSION = 2
    }
}
