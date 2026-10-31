package cn.puji.app.domain

object StockMath {
    fun nextStock(type: DocumentType, current: Int, quantity: Int): Result<Int> = runCatching {
        require(current >= 0) { "当前库存异常" }
        require(quantity > 0) { "数量必须大于 0" }
        when (type) {
            DocumentType.INBOUND -> Math.addExact(current, quantity)
            DocumentType.OUTBOUND -> {
                if (quantity > current) throw InsufficientStockException("")
                current - quantity
            }
            else -> throw IllegalArgumentException("请使用对应的库存操作")
        }
    }
}

