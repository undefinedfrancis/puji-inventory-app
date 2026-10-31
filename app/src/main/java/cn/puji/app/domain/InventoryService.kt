package cn.puji.app.domain

class EmptyDocumentException : IllegalArgumentException("请先添加商品")
class InsufficientStockException(val productName: String) : IllegalStateException("$productName 库存不足")

object InventoryService {
    fun validate(cart: Cart, checkStockSnapshot: Boolean = true): Result<Unit> = runCatching {
        require(cart.type == DocumentType.INBOUND || cart.type == DocumentType.OUTBOUND) { "请选择入货或出货" }
        if (cart.lines.isEmpty()) throw EmptyDocumentException()
        var total = 0L
        var quantity = 0
        cart.lines.forEach { line ->
            require(line.quantity > 0) { "数量必须大于 0" }
            require(line.unitPriceCents >= 0) { "单价不能为负数" }
            total = Math.addExact(total, Math.multiplyExact(line.unitPriceCents, line.quantity.toLong()))
            quantity = Math.addExact(quantity, line.quantity)
            if (checkStockSnapshot && cart.type == DocumentType.OUTBOUND && line.quantity > line.product.stockQuantity) {
                throw InsufficientStockException(line.product.name)
            }
        }
    }
}
