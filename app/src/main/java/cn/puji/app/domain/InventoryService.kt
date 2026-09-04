package cn.puji.app.domain

class EmptyDocumentException : IllegalArgumentException("请先添加商品")
class InsufficientStockException(val productName: String) :
    IllegalStateException("$productName 库存不足")

object InventoryService {
    fun validate(cart: Cart): Result<Unit> = runCatching {
        if (cart.lines.isEmpty()) throw EmptyDocumentException()
        cart.lines.forEach { line ->
            require(line.quantity > 0) { "数量必须大于 0" }
            require(line.unitPriceCents >= 0) { "单价不能为负数" }
            if (cart.type == DocumentType.OUTBOUND && line.quantity > line.product.stockQuantity) {
                throw InsufficientStockException(line.product.name)
            }
        }
    }
}

