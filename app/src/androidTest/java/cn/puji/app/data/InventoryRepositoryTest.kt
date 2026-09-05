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
}
