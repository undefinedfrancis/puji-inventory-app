package cn.puji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartTest {
    private val cola = Product(
        id = 1,
        name = "可口可乐 500ml",
        barcode = "6901234567890",
        category = "饮料",
        costPriceCents = 220,
        salePriceCents = 350,
        stockQuantity = 12,
        lowStockThreshold = 5,
    )

    @Test fun add_sameProduct_mergesQuantity_andUsesSalePrice() {
        val cart = Cart.empty(DocumentType.OUTBOUND).add(cola).add(cola)
        assertEquals(2, cart.lines.single().quantity)
        assertEquals(700L, cart.totalCents)
    }

    @Test fun inbound_usesCostPrice_andAllowsPriceOverride() {
        val cart = Cart.empty(DocumentType.INBOUND).add(cola).updateUnitPrice(cola.id, 200)
        assertEquals(200L, cart.totalCents)
    }

    @Test fun decrement_removesLastUnit() {
        val cart = Cart.empty(DocumentType.OUTBOUND).add(cola).decrement(cola.id)
        assertTrue(cart.lines.isEmpty())
    }
}

