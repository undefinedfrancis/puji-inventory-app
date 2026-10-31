package cn.puji.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.puji.app.domain.Cart
import cn.puji.app.domain.DateRange
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Product
import cn.puji.app.scanner.ScannedProductRegistration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var database: PujiDatabase
    private lateinit var products: ProductRepository
    private lateinit var inventory: InventoryRepository

    @Before fun setUp() {
        context.deleteDatabase("puji.db")
        database = PujiDatabase(context)
        products = ProductRepository(database)
        inventory = InventoryRepository(database)
        // Fixtures are explicit test data; fresh production databases remain empty.
        listOf(
            arrayOf<Any>("可口可乐 500ml","6901234567890","饮料",220,350,18,5,"",1,1,java.util.UUID.randomUUID().toString(),3960),
            arrayOf<Any>("农夫山泉 550ml","6901234567891","饮料",100,200,4,6,"",1,1,java.util.UUID.randomUUID().toString(),400),
            arrayOf<Any>("康师傅红烧牛肉面","6901234567892","食品",320,450,24,8,"",1,1,java.util.UUID.randomUUID().toString(),7680),
        ).forEach { args -> database.writableDatabase.execSQL(
            "INSERT INTO products(name,barcode,category,cost_price_cents,sale_price_cents,stock_quantity,low_stock_threshold,note,created_at,updated_at,uuid,stock_cost_cents) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",args,
        ) }
    }

    @After fun tearDown() { database.close(); context.deleteDatabase("puji.db") }

    @Test fun inboundAndOutbound_updateStockAndKeepDocumentSnapshots() {
        val original = products.list().first()
        inventory.commit(Cart.empty(DocumentType.INBOUND).add(original)).getOrThrow()
        val afterInbound = products.find(original.id)!!
        assertEquals(original.stockQuantity + 1, afterInbound.stockQuantity)

        inventory.commit(Cart.empty(DocumentType.OUTBOUND).add(afterInbound)).getOrThrow()
        assertEquals(original.stockQuantity, products.find(original.id)!!.stockQuantity)
        assertEquals(2, inventory.documents().size)
        assertEquals(original.name, inventory.documents().last().lines.single().productName)
    }

    @Test fun duplicateBarcode_isRejected() {
        val first = products.list().first()
        val result = products.save(first.copy(id = 0, name = "重复商品"))
        assertTrue(result.exceptionOrNull() is DuplicateBarcodeException)
    }

    @Test fun dashboard_doesNotRepeatDocumentTotalForEachLine() {
        val available = products.list().take(2)
        val cart = available.fold(Cart.empty(DocumentType.OUTBOUND)) { current, product ->
            current.add(product)
        }

        inventory.commit(cart).getOrThrow()

        val stats = DashboardRepository(database).stats()
        assertEquals(cart.totalCents, stats.todaySalesCents)
        assertEquals(cart.totalQuantity, stats.todayItemCount)
    }

    @Test fun outbound_keepsCostSnapshotAfterProductPriceChanges() {
        val product = products.list().first()
        inventory.commit(Cart.empty(DocumentType.OUTBOUND).add(product)).getOrThrow()

        products.save(product.copy(costPriceCents = product.costPriceCents + 100)).getOrThrow()

        assertEquals(
            product.costPriceCents,
            inventory.documents().single().lines.single().costPriceSnapshotCents,
        )
    }

    @Test fun documents_rangeIncludesStartAndExcludesEnd() {
        var now = 1_000L
        val timedInventory = InventoryRepository(database) { now }
        val product = products.list().first()
        timedInventory.commit(Cart.empty(DocumentType.OUTBOUND).add(product)).getOrThrow()
        now = 1_500L
        timedInventory.commit(Cart.empty(DocumentType.OUTBOUND).add(product)).getOrThrow()
        now = 2_000L
        timedInventory.commit(Cart.empty(DocumentType.OUTBOUND).add(product)).getOrThrow()

        val results = timedInventory.documents(range = DateRange(1_000L, 2_000L, "测试范围"))

        assertEquals(listOf(1_500L, 1_000L), results.map { it.createdAt })
    }

    @Test fun products_canBeFilteredByNormalizedCategory() {
        val drink = products.list().first { it.name.startsWith("可口可乐") }
        products.save(drink.copy(category = " ")).getOrThrow()

        assertTrue(products.list(category = "饮料").all { it.category == "饮料" })
        assertEquals(listOf(drink.id), products.list(category = "未分类").map { it.id })
        assertEquals(
            listOf("农夫山泉 550ml"),
            products.list(query = "农夫", category = "饮料").map { it.name },
        )
        assertTrue(products.list(query = "面", category = "饮料").isEmpty())
    }

    @Test fun scannedInboundRegistration_doesNotAddAnExtraItem() {
        val entered = Product(
            name = "新商品",
            barcode = "88990011",
            costPriceCents = 200,
            salePriceCents = 300,
            stockQuantity = 5,
        )
        val plan = ScannedProductRegistration.plan(entered, DocumentType.INBOUND)
        val id = products.save(plan.productToSave).getOrThrow()
        val saved = products.find(id)!!

        inventory.commit(ScannedProductRegistration.transactionCart(saved, plan)).getOrThrow()

        assertEquals(5, products.find(id)!!.stockQuantity)
        assertEquals(5, inventory.documents().single().lines.single().quantity)
    }
}
