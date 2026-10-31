package cn.puji.app.domain

import org.junit.Assert.*
import org.junit.Test

class CartSafetyTest {
    private val product = Product(id = 1, name = "商品", salePriceCents = Long.MAX_VALUE)

    @Test fun cartLineTotalDoesNotWrap() {
        assertThrows(ArithmeticException::class.java) { CartLine(product, 2, Long.MAX_VALUE).totalCents }
    }

    @Test fun mergedQuantityDoesNotWrap() {
        assertThrows(ArithmeticException::class.java) {
            Cart.empty(DocumentType.OUTBOUND).add(product, Int.MAX_VALUE).add(product)
        }
    }

    @Test fun yuanFormattingKeepsLargeIntegerCentsExact() {
        assertTrue(Money.format(9_007_199_254_740_991L).endsWith("409.91"))
    }
}
