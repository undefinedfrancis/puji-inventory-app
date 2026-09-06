package cn.puji.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.puji.app.domain.Cart
import cn.puji.app.domain.DocumentType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppResetRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var database: PujiDatabase
    private lateinit var products: ProductRepository

    @Before fun setUp() {
        context.deleteDatabase("puji.db")
        database = PujiDatabase(context)
        products = ProductRepository(database)
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase("puji.db")
    }

    @Test fun clearAll_removesProductsDocumentsLinesMovementsAndSequences() {
        val product = products.list().first()
        InventoryRepository(database).commit(Cart.empty(DocumentType.OUTBOUND).add(product)).getOrThrow()

        AppResetRepository(database).clearAll().getOrThrow()

        listOf("products", "documents", "document_lines", "stock_movements").forEach { table ->
            assertEquals(0, count(table))
        }
        assertEquals(0, count("sqlite_sequence"))
    }

    @Test fun clearAll_rollsBackEverythingWhenAnyDeleteFails() {
        val product = products.list().first()
        InventoryRepository(database).commit(Cart.empty(DocumentType.OUTBOUND).add(product)).getOrThrow()
        val before = listOf("products", "documents", "document_lines", "stock_movements").associateWith(::count)
        database.writableDatabase.execSQL(
            "CREATE TEMP TRIGGER fail_product_delete BEFORE DELETE ON products BEGIN SELECT RAISE(ABORT, 'blocked'); END",
        )

        val result = AppResetRepository(database).clearAll()

        assertTrue(result.isFailure)
        before.forEach { (table, expected) -> assertEquals(expected, count(table)) }
    }

    private fun count(table: String): Int = database.readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM $table",
        null,
    ).use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }
}
