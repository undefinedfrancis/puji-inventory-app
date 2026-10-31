package cn.puji.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

class PujiDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE products (
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, barcode TEXT,
            category TEXT NOT NULL DEFAULT '未分类', cost_price_cents INTEGER NOT NULL DEFAULT 0,
            sale_price_cents INTEGER NOT NULL DEFAULT 0, stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK(stock_quantity>=0),
            low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK(low_stock_threshold>=0), note TEXT NOT NULL DEFAULT '',
            active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
            uuid TEXT NOT NULL, stock_cost_cents INTEGER NOT NULL DEFAULT 0 CHECK(stock_cost_cents>=0),
            version INTEGER NOT NULL DEFAULT 1, source TEXT NOT NULL DEFAULT 'local',
            cost_source TEXT NOT NULL DEFAULT 'actual', migration_stock_cost_cents INTEGER NOT NULL DEFAULT 0
        )""")
        createDocuments(db, "documents")
        db.execSQL("""CREATE TABLE document_lines (
            id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER NOT NULL REFERENCES documents(id),
            product_id INTEGER NOT NULL REFERENCES products(id), product_name_snapshot TEXT NOT NULL,
            barcode_snapshot TEXT NOT NULL DEFAULT '', unit_price_cents INTEGER NOT NULL,
            cost_price_snapshot_cents INTEGER NOT NULL DEFAULT 0, quantity INTEGER NOT NULL CHECK(quantity>0),
            line_total_cents INTEGER NOT NULL, uuid TEXT NOT NULL, line_cost_cents INTEGER NOT NULL DEFAULT 0,
            stock_cost_delta_cents INTEGER NOT NULL DEFAULT 0, quantity_delta INTEGER NOT NULL DEFAULT 0,
            original_line_id INTEGER REFERENCES document_lines(id), valuation_difference_cents INTEGER NOT NULL DEFAULT 0,
            cost_source TEXT NOT NULL DEFAULT 'actual', version INTEGER NOT NULL DEFAULT 1, source TEXT NOT NULL DEFAULT 'local'
        )""")
        db.execSQL("""CREATE TABLE stock_movements (
            id INTEGER PRIMARY KEY AUTOINCREMENT, product_id INTEGER NOT NULL REFERENCES products(id),
            document_id INTEGER NOT NULL REFERENCES documents(id), type TEXT NOT NULL, quantity_delta INTEGER NOT NULL,
            stock_after INTEGER NOT NULL, created_at INTEGER NOT NULL, uuid TEXT NOT NULL,
            stock_cost_delta_cents INTEGER NOT NULL DEFAULT 0, stock_cost_after_cents INTEGER NOT NULL DEFAULT 0,
            cost_source TEXT NOT NULL DEFAULT 'actual', version INTEGER NOT NULL DEFAULT 1, source TEXT NOT NULL DEFAULT 'local'
        )""")
        createIndexes(db)
    }

    private fun createDocuments(db: SQLiteDatabase, name: String) {
        db.execSQL("""CREATE TABLE $name (
            id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, total_cents INTEGER NOT NULL,
            note TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, uuid TEXT NOT NULL,
            request_id TEXT NOT NULL, original_document_id INTEGER REFERENCES documents(id), original_type TEXT,
            valuation_difference_cents INTEGER NOT NULL DEFAULT 0, cost_source TEXT NOT NULL DEFAULT 'actual',
            version INTEGER NOT NULL DEFAULT 1, source TEXT NOT NULL DEFAULT 'local',
            expected_quantity INTEGER, actual_quantity INTEGER
        )""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE document_lines ADD COLUMN cost_price_snapshot_cents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("""UPDATE document_lines SET cost_price_snapshot_cents=CASE
                WHEN (SELECT type FROM documents WHERE id=document_lines.document_id)='INBOUND' THEN unit_price_cents
                ELSE COALESCE((SELECT cost_price_cents FROM products WHERE id=document_lines.product_id),0) END""")
        }
        if (oldVersion < 3) {
            val costSource = if (oldVersion < 2) "historical_estimate" else "historical_unspecified"
            addColumns(db, "products", listOf(
                "uuid TEXT NOT NULL DEFAULT ''", "stock_cost_cents INTEGER NOT NULL DEFAULT 0",
                "version INTEGER NOT NULL DEFAULT 1", "source TEXT NOT NULL DEFAULT 'migration'",
                "cost_source TEXT NOT NULL DEFAULT 'migration_estimate'", "migration_stock_cost_cents INTEGER NOT NULL DEFAULT 0",
            ))
            // Evaluate in Kotlin so overflowing legacy data cannot silently become SQLite REAL values.
            db.rawQuery("SELECT id,stock_quantity,cost_price_cents FROM products",null).use { cursor ->
                while(cursor.moveToNext()) {
                    val cost=Math.multiplyExact(cursor.getLong(1),cursor.getLong(2))
                    require(cost>=0) { "旧库存成本异常" }
                    db.execSQL("UPDATE products SET stock_cost_cents=?,migration_stock_cost_cents=? WHERE id=?",arrayOf(cost,cost,cursor.getLong(0)))
                }
            }
            // The original CHECK only permits INBOUND/OUTBOUND. Copy all original values and primary keys.
            createDocuments(db,"documents_v3")
            db.execSQL("""INSERT INTO documents_v3(id,type,total_cents,note,created_at,uuid,request_id,cost_source,source)
                SELECT id,type,total_cents,note,created_at,'','legacy-'||id,'$costSource','migration' FROM documents""")
            db.execSQL("DROP TABLE documents")
            db.execSQL("ALTER TABLE documents_v3 RENAME TO documents")
            addColumns(db,"document_lines",listOf(
                "uuid TEXT NOT NULL DEFAULT ''", "line_cost_cents INTEGER NOT NULL DEFAULT 0",
                "stock_cost_delta_cents INTEGER NOT NULL DEFAULT 0", "quantity_delta INTEGER NOT NULL DEFAULT 0",
                "original_line_id INTEGER REFERENCES document_lines(id)", "valuation_difference_cents INTEGER NOT NULL DEFAULT 0",
                "cost_source TEXT NOT NULL DEFAULT '$costSource'", "version INTEGER NOT NULL DEFAULT 1", "source TEXT NOT NULL DEFAULT 'migration'",
            ))
            db.rawQuery("SELECT l.id,l.cost_price_snapshot_cents,l.quantity,d.type FROM document_lines l JOIN documents d ON d.id=l.document_id",null).use { c ->
                while(c.moveToNext()) {
                    val cost=Math.multiplyExact(c.getLong(1),c.getLong(2)); val sign=if(c.getString(3)=="INBOUND") 1 else -1
                    db.execSQL("UPDATE document_lines SET line_cost_cents=?,stock_cost_delta_cents=?,quantity_delta=? WHERE id=?",arrayOf<Any>(cost,Math.multiplyExact(cost,sign.toLong()),c.getInt(2)*sign,c.getLong(0)))
                }
            }
            addColumns(db,"stock_movements",listOf(
                "uuid TEXT NOT NULL DEFAULT ''", "stock_cost_delta_cents INTEGER NOT NULL DEFAULT 0",
                "stock_cost_after_cents INTEGER NOT NULL DEFAULT 0", "cost_source TEXT NOT NULL DEFAULT '$costSource'",
                "version INTEGER NOT NULL DEFAULT 1", "source TEXT NOT NULL DEFAULT 'migration'",
            ))
            // Legacy movement balances had no stored valuation; keep that absence explicit in cost_source.
            listOf("products","documents","document_lines","stock_movements").forEach { table ->
                db.rawQuery("SELECT id FROM $table",null).use { c ->
                    while(c.moveToNext()) db.execSQL("UPDATE $table SET uuid=? WHERE id=?",arrayOf<Any>(UUID.randomUUID().toString(),c.getLong(0)))
                }
            }
            createIndexes(db)
        }
    }

    private fun addColumns(db:SQLiteDatabase, table:String, columns:List<String>) = columns.forEach { db.execSQL("ALTER TABLE $table ADD COLUMN $it") }
    private fun createIndexes(db:SQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL AND barcode<>''")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_name ON products(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_created ON documents(created_at DESC,id DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_type_created ON documents(type,created_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lines_document ON document_lines(document_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lines_original ON document_lines(original_line_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_original ON documents(original_document_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_movements_product ON stock_movements(product_id,created_at DESC,id DESC)")
        listOf("products","documents","document_lines","stock_movements").forEach { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_${it}_uuid ON $it(uuid)") }
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_documents_request ON documents(request_id)")
    }
    companion object { const val DATABASE_NAME="puji.db"; const val DATABASE_VERSION=3 }
}
