package cn.puji.app.ui.product

import cn.puji.app.domain.Money

data class ProductForm(
    val name: String = "",
    val barcode: String = "",
    val category: String = "",
    val costPrice: String = "",
    val salePrice: String = "",
    val stock: String = "0",
    val threshold: String = "5",
    val note: String = "",
)

enum class ProductField { NAME, COST_PRICE, SALE_PRICE, STOCK, THRESHOLD }

object ProductFormValidator {
    fun validate(form: ProductForm, minimumStock: Int = 0): Map<ProductField, String> = buildMap {
        if (form.name.isBlank()) put(ProductField.NAME, "请输入商品名称")
        if (Money.parseYuan(form.costPrice).isFailure) put(ProductField.COST_PRICE, "请输入正确的进货价")
        if (Money.parseYuan(form.salePrice).isFailure) put(ProductField.SALE_PRICE, "请输入正确的售价")
        val stock = form.stock.toIntOrNull()
        if (stock == null || stock < minimumStock) {
            put(
                ProductField.STOCK,
                if (minimumStock == 1) "本次入货数量至少为 1" else "库存必须是非负整数",
            )
        }
        val threshold = form.threshold.toIntOrNull()
        if (threshold == null || threshold < 0) put(ProductField.THRESHOLD, "预警值必须是非负整数")
    }
}
