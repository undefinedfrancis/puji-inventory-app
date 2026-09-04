package cn.puji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StockMathTest {
    @Test fun inbound_adds_andOutboundSubtracts() {
        assertEquals(15, StockMath.nextStock(DocumentType.INBOUND, 10, 5).getOrThrow())
        assertEquals(7, StockMath.nextStock(DocumentType.OUTBOUND, 10, 3).getOrThrow())
    }

    @Test fun outbound_cannotMakeStockNegative() {
        assertTrue(StockMath.nextStock(DocumentType.OUTBOUND, 1, 2).isFailure)
    }

    @Test fun quantity_mustBePositive() {
        assertTrue(StockMath.nextStock(DocumentType.INBOUND, 1, 0).isFailure)
    }
}
