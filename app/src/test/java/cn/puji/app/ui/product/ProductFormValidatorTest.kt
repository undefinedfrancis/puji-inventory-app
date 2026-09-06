package cn.puji.app.ui.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFormValidatorTest {
    @Test fun validate_rejectsBlankNameInvalidMoneyAndStock() {
        val errors = ProductFormValidator.validate(
            ProductForm(name = " ", costPrice = "1.999", salePrice = "-2", stock = "1.5", threshold = "-1"),
        )
        assertEquals("请输入商品名称", errors[ProductField.NAME])
        assertTrue(ProductField.COST_PRICE in errors)
        assertTrue(ProductField.SALE_PRICE in errors)
        assertTrue(ProductField.STOCK in errors)
        assertTrue(ProductField.THRESHOLD in errors)
    }

    @Test fun scannedInbound_requiresAtLeastOneItem() {
        val form = ProductForm(name = "牛奶", costPrice = "2.00", salePrice = "3.00", stock = "0")

        val errors = ProductFormValidator.validate(form, minimumStock = 1)

        assertEquals("本次入货数量至少为 1", errors[ProductField.STOCK])
    }
}
