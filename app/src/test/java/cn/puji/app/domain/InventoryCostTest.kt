package cn.puji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryCostTest {
    @Test fun lastItemConsumesRemainder() {
        assertEquals(33L, InventoryCost.allocate(100, 3, 1))
        assertEquals(33L, InventoryCost.allocate(67, 2, 1))
        assertEquals(34L, InventoryCost.allocate(34, 1, 1))
    }
    @Test fun partialReturnsAllocateByCumulativeQuantity() {
        assertEquals(33L, InventoryCost.returnCost(100, 3, 0, 1))
        assertEquals(33L, InventoryCost.returnCost(100, 3, 1, 1))
        assertEquals(34L, InventoryCost.returnCost(100, 3, 2, 1))
    }
    @Test fun allocationAvoidsIntermediateOverflow() {
        assertEquals(Long.MAX_VALUE, InventoryCost.allocate(Long.MAX_VALUE, 2, 2))
        assertEquals(Long.MAX_VALUE / 2, InventoryCost.allocate(Long.MAX_VALUE, 2, 1))
    }
    @Test(expected = IllegalArgumentException::class)
    fun returnsCannotExceedOriginalQuantity() { InventoryCost.returnCost(100, 3, 2, 2) }
    @Test fun returnCostUsesActiveActualCostAfterAnEarlierReturnIsReversed() {
        assertEquals(0L,InventoryCost.returnCost(2,3,0,1,0))
        assertEquals(1L,InventoryCost.returnCost(2,3,1,1,0))
        // The first zero-cost return is reversed: one active item still owns one cent.
        assertEquals(0L,InventoryCost.returnCost(2,3,1,1,1))
        assertEquals(1L,InventoryCost.returnCost(2,3,2,1,1))
    }
    @Test fun activeCostAheadOfQuantityTargetNeverProducesNegativeCostAndFinalReturnOwnsRemainder() {
        assertEquals(0L,InventoryCost.returnCost(2,5,1,1,1))
        assertEquals(1L,InventoryCost.returnCost(2,5,2,3,1))
    }
    @Test(expected=IllegalArgumentException::class)
    fun activeReturnedCostCannotExceedOriginalCost() { InventoryCost.returnCost(2,3,1,1,3) }

}
