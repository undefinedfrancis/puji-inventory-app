package cn.puji.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryServiceTest {
    @Test fun outbound_rejectsQuantityAboveStock() {
        val product = Product(id = 1, name = "矿泉水", salePriceCents = 200, stockQuantity = 1)
        val cart = Cart.empty(DocumentType.OUTBOUND).add(product).increment(product.id)
        val result = InventoryService.validate(cart)
        assertTrue(result.exceptionOrNull() is InsufficientStockException)
    }

    @Test fun inbound_acceptsPositiveQuantity() {
        val product = Product(id = 1, name = "矿泉水", costPriceCents = 100)
        assertTrue(InventoryService.validate(Cart.empty(DocumentType.INBOUND).add(product)).isSuccess)
    }
}

