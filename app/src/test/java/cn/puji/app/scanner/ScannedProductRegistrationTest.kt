package cn.puji.app.scanner

import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannedProductRegistrationTest {
    private val enteredProduct = Product(
        name = "新商品",
        barcode = "6909",
        costPriceCents = 200,
        salePriceCents = 300,
        stockQuantity = 5,
    )

    @Test fun inboundPlan_savesNewProductAtZeroThenReceivesExactlyEnteredQuantity() {
        val plan = ScannedProductRegistration.plan(enteredProduct, DocumentType.INBOUND)

        assertEquals(0, plan.productToSave.stockQuantity)
        assertEquals(5, plan.transactionQuantity)
        assertTrue(plan.completeTransactionOnSave)

        val saved = plan.productToSave.copy(id = 42)
        val cart = ScannedProductRegistration.transactionCart(saved, plan)
        assertEquals(5, cart.totalQuantity)
    }

    @Test fun outboundPlan_keepsExistingStockAndUsesNormalConfirmationFlow() {
        val plan = ScannedProductRegistration.plan(enteredProduct, DocumentType.OUTBOUND)

        assertEquals(5, plan.productToSave.stockQuantity)
        assertFalse(plan.completeTransactionOnSave)
    }
}
