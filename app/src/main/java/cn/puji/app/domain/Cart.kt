package cn.puji.app.domain

data class CartLine(
    val product: Product,
    val quantity: Int,
    val unitPriceCents: Long,
) {
    val totalCents: Long get() = Math.multiplyExact(unitPriceCents, quantity.toLong())
}

data class Cart(
    val type: DocumentType,
    val lines: List<CartLine> = emptyList(),
    val note: String = "",
) {
    val totalCents: Long get() = lines.fold(0L) { total, line -> Math.addExact(total, line.totalCents) }
    val totalQuantity: Int get() = lines.fold(0) { total, line -> Math.addExact(total, line.quantity) }

    fun add(product: Product, quantity: Int = 1): Cart {
        require(type == DocumentType.INBOUND || type == DocumentType.OUTBOUND) { "请选择入货或出货" }
        require(quantity > 0) { "数量必须大于 0" }
        val existing = lines.firstOrNull { it.product.id == product.id }
        val defaultPrice = if (type == DocumentType.INBOUND) product.costPriceCents else product.salePriceCents
        return if (existing == null) {
            copy(lines = lines + CartLine(product, quantity, defaultPrice))
        } else {
            copy(lines = lines.map { if (it.product.id == product.id) it.copy(quantity = Math.addExact(it.quantity, quantity)) else it })
        }
    }

    fun increment(productId: Long): Cart = copy(
        lines = lines.map { if (it.product.id == productId) it.copy(quantity = Math.addExact(it.quantity, 1)) else it },
    )

    fun decrement(productId: Long): Cart = copy(
        lines = lines.mapNotNull {
            when {
                it.product.id != productId -> it
                it.quantity > 1 -> it.copy(quantity = it.quantity - 1)
                else -> null
            }
        },
    )

    fun updateUnitPrice(productId: Long, cents: Long): Cart {
        require(cents >= 0)
        return copy(lines = lines.map { if (it.product.id == productId) it.copy(unitPriceCents = cents) else it })
    }

    fun remove(productId: Long): Cart = copy(lines = lines.filterNot { it.product.id == productId })

    companion object {
        fun empty(type: DocumentType) = Cart(type)
    }
}
