package cn.puji.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.puji.app.backup.BackupJson
import cn.puji.app.domain.Cart
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Product
import cn.puji.app.importer.ProductCsvImporter
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var db: PujiDatabase
    private lateinit var products: ProductRepository
    private lateinit var backup: BackupRepository

    @Before fun setUp() {
        context.deleteDatabase(PujiDatabase.DATABASE_NAME)
        db = PujiDatabase(context)
        products = ProductRepository(db)
        backup = BackupRepository(db)
    }
    @After fun tearDown() { db.close(); context.deleteDatabase(PujiDatabase.DATABASE_NAME) }

    @Test fun completeRoundTripPreservesEveryColumnIncludingOpeningAndReturns() {
        val id = products.save(Product(name = "可乐", barcode = "001", costPriceCents = 220, salePriceCents = 350, stockQuantity = 10)).getOrThrow()
        val inventory = InventoryRepository(db)
        val sale = inventory.commit(Cart.empty(DocumentType.OUTBOUND).add(products.find(id)!!), "backup-test-sale").getOrThrow()
        val line = inventory.documents().first { it.id == sale }.lines.single()
        inventory.returnLines(sale, mapOf(line.id to 1), "backup-test-return").getOrThrow()
        val file = backup.exportJson()
        assertEquals(1, backup.inspect(file).productCount)
        assertEquals(3, backup.inspect(file).documentCount)
        products.save(Product(name = "待替换", barcode = "002")).getOrThrow()
        backup.restoreJson(file).getOrThrow()
        assertEquals(tables(file), tables(backup.exportJson()))
    }

    @Test fun corruptedFileAndFailedInsertLeaveCurrentDataUnchanged() {
        products.save(Product(name = "保留商品", barcode = "001", stockQuantity = 2, costPriceCents = 200)).getOrThrow()
        val file = backup.exportJson()
        assertTrue(backup.restoreJson(file.replace("保留商品", "篡改商品")).isFailure)
        assertEquals("保留商品", products.list().single().name)
        db.writableDatabase.execSQL("CREATE TRIGGER reject_restore BEFORE INSERT ON products BEGIN SELECT RAISE(ABORT,'test restore failure'); END")
        assertTrue(backup.restoreJson(file).isFailure)
        assertEquals(tables(file), tables(backup.exportJson()))
    }

    @Test fun csvUpdatesInactiveProductDetailsWithoutChangingStockOrCostAndCreatesOpening() {
        val id = products.save(Product(name = "旧名称", barcode = "001", stockQuantity = 8, costPriceCents = 200)).getOrThrow()
        products.deactivate(id).getOrThrow()
        val csv = ProductCsvImporter.template() + "新名称,001,饮料,3.00,4.00,999,2,更新\r\n新品,002,饮料,1.00,2.00,5,1,期初\r\n"
        val imported = ProductCsvImporter.importRows(products, ProductCsvImporter.preview(csv)).getOrThrow()
        assertEquals(1, imported.created)
        assertEquals(1, imported.updated)
        val existing = products.find(id)!!
        assertEquals("新名称", existing.name)
        assertFalse(existing.active)
        assertEquals(8, existing.stockQuantity)
        assertEquals(1600L, existing.stockCostCents)
        val created = products.findByBarcode("002")!!
        assertEquals(5, created.stockQuantity)
        assertEquals(500L, created.stockCostCents)
        assertEquals(2, InventoryRepository(db).documents().count { it.type == DocumentType.OPENING })
    }

    @Test fun productBatchFailureRollsBackEarlierChanges() {
        val id = products.save(Product(name = "保留名称", barcode = "001", stockQuantity = 8, costPriceCents = 200)).getOrThrow()
        db.writableDatabase.execSQL("CREATE TRIGGER reject_new_import BEFORE INSERT ON products WHEN NEW.barcode='002' BEGIN SELECT RAISE(ABORT,'test import failure'); END")
        val csv = ProductCsvImporter.template() + "更改名称,001,饮料,3.00,4.00,0,2,\r\n失败新品,002,饮料,1.00,2.00,5,1,\r\n"
        assertTrue(ProductCsvImporter.importRows(products, ProductCsvImporter.preview(csv)).isFailure)
        assertEquals("保留名称", products.find(id)!!.name)
        assertEquals(1, products.list().size)
    }

    @Test fun scannerFeedbackRoundTripsWithoutIncludingDraftPreferences() {
        val preferences = context.getSharedPreferences("puji_backup_test_settings", android.content.Context.MODE_PRIVATE)
        preferences.edit().clear().putBoolean("scanner_feedback", false).putString("draft", "must not export").commit()
        val withSettings = BackupRepository(db, preferences)
        val file = withSettings.exportJson()
        assertFalse(file.contains("must not export"))
        preferences.edit().putBoolean("scanner_feedback", true).commit()
        withSettings.restoreJson(file).getOrThrow()
        assertFalse(preferences.getBoolean("scanner_feedback", true))
        assertEquals("must not export", preferences.getString("draft", null))
        preferences.edit().clear().commit()
    }

    private fun tables(content: String): Any? {
        val root = BackupJson.parse(content) as Map<*, *>
        return (root["payload"] as Map<*, *>)["tables"]
    }
}
