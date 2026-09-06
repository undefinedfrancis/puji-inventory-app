package cn.puji.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PujiDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun setUp() {
        context.deleteDatabase("puji.db")
    }

    @After fun tearDown() {
        context.deleteDatabase("puji.db")
    }

    @Test fun versionOneMigration_preservesRowsAndBackfillsCosts() {
        createVersionOneDatabase()

        PujiDatabase(context).use { helper ->
            val db = helper.writableDatabase
            val snapshots = db.rawQuery(
                "SELECT cost_price_snapshot_cents FROM document_lines ORDER BY id",
                null,
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getLong(0))
                }
            }
            assertEquals(listOf(180L, 125L), snapshots)
            assertEquals(1L, count(db, "products"))
            assertEquals(2L, count(db, "documents"))
        }
    }

    private fun createVersionOneDatabase() {
        context.openOrCreateDatabase("puji.db", Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                """CREATE TABLE products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    barcode TEXT,
                    category TEXT NOT NULL DEFAULT '未分类',
                    cost_price_cents INTEGER NOT NULL DEFAULT 0,
                    sale_price_cents INTEGER NOT NULL DEFAULT 0,
                    stock_quantity INTEGER NOT NULL DEFAULT 0,
                    low_stock_threshold INTEGER NOT NULL DEFAULT 5,
                    note TEXT NOT NULL DEFAULT '',
                    active INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )""".trimIndent(),
            )
            db.execSQL(
                """CREATE TABLE documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    total_cents INTEGER NOT NULL,
                    note TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL
                )""".trimIndent(),
            )
            db.execSQL(
                """CREATE TABLE document_lines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    document_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    product_name_snapshot TEXT NOT NULL,
                    barcode_snapshot TEXT NOT NULL DEFAULT '',
                    unit_price_cents INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    line_total_cents INTEGER NOT NULL
                )""".trimIndent(),
            )
            db.execSQL(
                """CREATE TABLE stock_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    document_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    quantity_delta INTEGER NOT NULL,
                    stock_after INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )""".trimIndent(),
            )
            db.execSQL("INSERT INTO products VALUES(1,'测试商品','6900','测试',125,200,10,2,'',1,1,1)")
            db.execSQL("INSERT INTO documents VALUES(1,'INBOUND',180,'',1)")
            db.execSQL("INSERT INTO documents VALUES(2,'OUTBOUND',200,'',2)")
            db.execSQL("INSERT INTO document_lines VALUES(1,1,1,'测试商品','6900',180,1,180)")
            db.execSQL("INSERT INTO document_lines VALUES(2,2,1,'测试商品','6900',200,1,200)")
            db.version = 1
        }
    }

    private fun count(db: android.database.sqlite.SQLiteDatabase, table: String): Long =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
}
