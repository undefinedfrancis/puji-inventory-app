package cn.puji.app.scanner

import cn.puji.app.domain.Cart
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Product

data class ScannedProductRegistrationPlan(
    val productToSave: Product,
    val type: DocumentType,
    val transactionQuantity: Int,
    val completeTransactionOnSave: Boolean,
)

object ScannedProductRegistration {
    fun plan(enteredProduct: Product, type: DocumentType): ScannedProductRegistrationPlan = when (type) {
        DocumentType.INBOUND -> {
            require(enteredProduct.stockQuantity > 0) { "本次入货数量至少为 1" }
            ScannedProductRegistrationPlan(
                productToSave = enteredProduct.copy(stockQuantity = 0),
                type = type,
                transactionQuantity = enteredProduct.stockQuantity,
                completeTransactionOnSave = true,
            )
        }
        DocumentType.OUTBOUND -> ScannedProductRegistrationPlan(
            productToSave = enteredProduct,
            type = type,
            transactionQuantity = 1,
            completeTransactionOnSave = false,
        )
        else -> throw IllegalArgumentException("请选择入货或出货")
    }

    fun transactionCart(
        savedProduct: Product,
        plan: ScannedProductRegistrationPlan,
    ): Cart = Cart.empty(plan.type).add(savedProduct, plan.transactionQuantity)
}
