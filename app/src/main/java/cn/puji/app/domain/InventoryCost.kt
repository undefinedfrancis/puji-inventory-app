package cn.puji.app.domain

import java.math.BigInteger

/** Integer cents are apportioned cumulatively; the last item always owns the remainder. */
object InventoryCost {
    fun allocate(totalCost: Long, stock: Int, quantity: Int): Long {
        require(totalCost >= 0 && stock > 0 && quantity in 1..stock) { "库存成本或数量异常" }
        return proportional(totalCost, quantity, stock)
    }
    /** Compatibility for a sequence with no reversed returns. */
    fun returnCost(originalCost: Long, originalQuantity: Int, alreadyReturned: Int, quantity: Int): Long {
        require(originalCost >= 0 && originalQuantity > 0 && alreadyReturned in 0..originalQuantity)
        return returnCost(originalCost,originalQuantity,alreadyReturned,quantity,
            proportional(originalCost,alreadyReturned,originalQuantity))
    }

    /**
     * Reversed returns remove their actual allocated cost, which can leave the remaining
     * active returns ahead of the new quantity-based target. Never refund a negative cost.
     * After each return active cost becomes max(previous actual cost, cumulative target),
     * hence stays <= original cost. At full quantity the target equals the original cost,
     * so the last return restores exactly the remaining cents, regardless of reversals.
     */
    fun returnCost(originalCost: Long, originalQuantity: Int, alreadyReturned: Int, quantity: Int, actualReturnedCost: Long): Long {
        require(originalCost >= 0 && originalQuantity > 0 && alreadyReturned >= 0 && quantity > 0)
        require(actualReturnedCost in 0..originalCost) { "原单已退成本异常" }
        val cumulative = Math.addExact(alreadyReturned, quantity)
        require(cumulative <= originalQuantity) { "退货数量超过原单可退数量" }
        val target=proportional(originalCost,cumulative,originalQuantity)
        return (target-actualReturnedCost).coerceAtLeast(0L)
    }
    private fun proportional(total: Long, part: Int, whole: Int): Long {
        val value = BigInteger.valueOf(total).multiply(BigInteger.valueOf(part.toLong()))
            .divide(BigInteger.valueOf(whole.toLong()))
        // BigInteger.longValueExact is only available from Android 12. Check before the
        // older Number conversion so Android 8+ keeps the same exact integer contract.
        check(value.signum() >= 0 && value.bitLength() <= 63) { "成本超出范围" }
        return value.toLong()
    }
}
