package cn.puji.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.puji.app.domain.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryLedgerTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var db: PujiDatabase
    private lateinit var products: ProductRepository
    private lateinit var ledger: InventoryRepository
    @Before fun setup() { context.deleteDatabase("puji.db"); db=PujiDatabase(context); products=ProductRepository(db); ledger=InventoryRepository(db) }
    @After fun close() { db.close(); context.deleteDatabase("puji.db") }
    private fun product(quantity:Int=0, cost:Long=100):Product {
        val id=products.save(Product(name="商品",costPriceCents=cost,salePriceCents=200,stockQuantity=quantity)).getOrThrow()
        return products.find(id)!!
    }
    @Test fun freshDatabaseIsEmptyAndOpeningDoesNotCountAsPurchase() {
        assertTrue(products.list().isEmpty())
        val p=product(3)
        assertEquals(300L,p.stockCostCents)
        assertEquals(DocumentType.OPENING,ledger.documents().single().type)
        assertEquals(0L,BusinessSummaryCalculator.calculate(ledger.documents()).purchaseExpenseCents)
    }
    @Test fun averageCostAndFinalRemainderIgnoreSuggestedPriceEdits() {
        var p=product()
        ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(p,3,33)))) .getOrThrow()
        p=products.find(p.id)!!
        ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(p,1,1)))).getOrThrow()
        products.save(p.copy(costPriceCents=999,stockQuantity=999)).getOrThrow()
        p=products.find(p.id)!!
        assertEquals(4,p.stockQuantity)
        assertEquals(100L,p.stockCostCents)
        repeat(4) { ledger.commit(Cart(DocumentType.OUTBOUND,listOf(CartLine(products.find(p.id)!!,1,200)))).getOrThrow() }
        assertEquals(0L,products.find(p.id)!!.stockCostCents)
        assertEquals(100L,ledger.documents(DocumentType.OUTBOUND).sumOf { it.lines.sumOf(DocumentLine::costTotalCents) })
    }
    @Test fun duplicateRequestDoesNotCreateSecondDocument() {
        val p=product()
        val cart=Cart.empty(DocumentType.INBOUND).add(p)
        assertEquals(ledger.commit(cart,"request").getOrThrow(),ledger.commit(cart,"request").getOrThrow())
        assertEquals(1,products.find(p.id)!!.stockQuantity)
    }
    @Test fun partialReturnsRespectOriginalCostAndLimit() {
        var p=product()
        ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(p,2,33)))).getOrThrow()
        ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(p,1,34)))).getOrThrow()
        p=products.find(p.id)!!
        val sale=ledger.commit(Cart(DocumentType.OUTBOUND,listOf(CartLine(p,3,200)))).getOrThrow()
        val line=ledger.documents().first { it.id==sale }.lines.single()
        repeat(3) { ledger.returnLines(sale,mapOf(line.id to 1),"return-$it").getOrThrow() }
        assertEquals(100L,products.find(p.id)!!.stockCostCents)
        assertTrue(ledger.returnLines(sale,mapOf(line.id to 1),"extra").isFailure)
        assertTrue(ledger.reverse(sale,"reverse-sale").isFailure)
        val summary=BusinessSummaryCalculator.calculate(ledger.documents())
        assertEquals(0L,summary.salesRevenueCents)
        assertEquals(0L,summary.grossProfitCents)
    }
    @Test fun purchaseReturnUsesCurrentCostAndOriginalRefundAmount() {
        val p=product()
        val first=ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(p,1,100)))).getOrThrow()
        ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(products.find(p.id)!!,1,300)))).getOrThrow()
        val line=ledger.documents().first { it.id==first }.lines.single()
        ledger.returnLines(first,mapOf(line.id to 1),"return").getOrThrow()
        assertEquals(200L,products.find(p.id)!!.stockCostCents)
        assertEquals(100L,ledger.documents(DocumentType.PURCHASE_RETURN).single().valuationDifferenceCents)
    }
    @Test fun conflictsAndFailedMultiLineTransactionLeaveNoPartialChanges() {
        val p=product(1)
        assertTrue(ledger.count(p.id,0,2,100,"盘点").isFailure)
        val before=ledger.documents().size
        val other=product()
        assertTrue(ledger.commit(Cart(DocumentType.OUTBOUND,listOf(CartLine(p,1,200),CartLine(other,1,200)))).isFailure)
        assertEquals(1,products.find(p.id)!!.stockQuantity)
        assertEquals(before,ledger.documents().size)
    }
    @Test fun unknownInboundProductCreationRollsBackWithInvalidLine() {
        val unknown=Product(name="新建",barcode="new")
        val missing=Product(id=999,name="失效")
        assertTrue(ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(unknown,2,100),CartLine(missing,1,100)))).isFailure)
        assertNull(products.findByBarcode("new"))
        assertTrue(ledger.documents().isEmpty())
    }
    @Test fun reversalProtectsDependenciesAndCanReverseAnActiveReturn() {
        val p=product(3)
        val sale=ledger.commit(Cart(DocumentType.OUTBOUND,listOf(CartLine(p,2,200)))).getOrThrow()
        val line=ledger.documents().first { it.id==sale }.lines.single()
        val returned=ledger.returnLines(sale,mapOf(line.id to 1),"return").getOrThrow()
        assertTrue(ledger.reverse(sale,"blocked").isFailure)
        ledger.reverse(returned,"undo-return").getOrThrow()
        ledger.reverse(sale,"undo-sale").getOrThrow()
        assertEquals(3,products.find(p.id)!!.stockQuantity)
        assertEquals(300L,products.find(p.id)!!.stockCostCents)
        assertTrue(ledger.reverse(sale,"again").isFailure)
        assertEquals(0L,BusinessSummaryCalculator.calculate(ledger.documents()).salesRevenueCents)
    }
    @Test fun inboundReversalCannotRemoveConsumedInventory() {
        val p=product()
        val inbound=ledger.commit(Cart.empty(DocumentType.INBOUND).add(p)).getOrThrow()
        ledger.commit(Cart.empty(DocumentType.OUTBOUND).add(products.find(p.id)!!)).getOrThrow()
        val before=ledger.documents().size
        assertTrue(ledger.reverse(inbound,"reverse").isFailure)
        assertEquals(before,ledger.documents().size)
    }
    @Test fun countWriteOffAndInactiveValuationDoNotChangePurchaseExpense() {
        var p=product(3)
        ledger.count(p.id,2,3,100,"少一件").getOrThrow()
        ledger.writeOff(p.id,1,"破损").getOrThrow()
        p=products.find(p.id)!!
        ledger.count(p.id,2,1,150,"补录").getOrThrow()
        products.deactivate(p.id).getOrThrow()
        val summary=BusinessSummaryCalculator.calculate(ledger.documents())
        assertEquals(0L,summary.purchaseExpenseCents)
        assertEquals(100L,summary.stockLossCostCents)
        assertEquals(100L,summary.writeOffCostCents)
        assertEquals(250L,DashboardRepository(db).stats().inventoryValueCents)
        assertEquals(4,ledger.stockHistory(p.id).size)
    }
    @Test fun bulkImportIsAtomicAndNeverOverwritesExistingStock() {
        var p=product(2)
        products.save(p.copy(barcode="same")).getOrThrow()
        p=products.find(p.id)!!
        products.importProducts(listOf(p.copy(name="改名",stockQuantity=999),Product(name="新商品",stockQuantity=1,costPriceCents=50))).getOrThrow()
        val existing=products.find(p.id)!!
        assertEquals(2,existing.stockQuantity)
        val count=products.list().size
        val documents=ledger.documents().size
        assertTrue(products.importProducts(listOf(Product(name="先写",stockQuantity=1),Product(name=""))).isFailure)
        assertEquals(count,products.list().size)
        assertEquals(documents,ledger.documents().size)
    }

    @Test fun reversingAnEarlierZeroCostPartialReturnCannotCreateAnExtraCent() {
        var p=product()
        ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(p,2,1)))).getOrThrow()
        ledger.commit(Cart(DocumentType.INBOUND,listOf(CartLine(p,1,0)))).getOrThrow()
        p=products.find(p.id)!!
        assertEquals(2L,p.stockCostCents)
        val sale=ledger.commit(Cart(DocumentType.OUTBOUND,listOf(CartLine(p,3,10)))).getOrThrow()
        val saleLine=ledger.findDocument(sale)!!.lines.single()
        assertEquals(2L,saleLine.lineCostCents)
        val first=ledger.returnLines(sale,mapOf(saleLine.id to 1),"first-return").getOrThrow()
        val second=ledger.returnLines(sale,mapOf(saleLine.id to 1),"second-return").getOrThrow()
        assertEquals(0L,ledger.findDocument(first)!!.lines.single().lineCostCents)
        assertEquals(1L,ledger.findDocument(second)!!.lines.single().lineCostCents)
        ledger.reverse(first,"reverse-first-return").getOrThrow()
        val active=ledger.findDocument(sale)!!.lines.single()
        assertEquals(1,active.returnedQuantity)
        assertEquals(1L,active.returnedCostCents)
        val third=ledger.returnLines(sale,mapOf(saleLine.id to 1),"third-return").getOrThrow()
        val last=ledger.returnLines(sale,mapOf(saleLine.id to 1),"last-return").getOrThrow()
        assertEquals(0L,ledger.findDocument(third)!!.lines.single().lineCostCents)
        assertEquals(1L,ledger.findDocument(last)!!.lines.single().lineCostCents)
        val fullyReturned=ledger.findDocument(sale)!!.lines.single()
        assertEquals(3,fullyReturned.returnedQuantity)
        assertEquals(2L,fullyReturned.returnedCostCents)
        assertEquals(2L,ledger.documents(DocumentType.OUTBOUND).single().lines.single().returnedCostCents)
        assertEquals(3,products.find(p.id)!!.stockQuantity)
        assertEquals(2L,products.find(p.id)!!.stockCostCents)
        val summary=BusinessSummaryCalculator.calculate(ledger.documents())
        assertEquals(0L,summary.salesCostCents)
        assertEquals(0L,summary.grossProfitCents)
    }

}
