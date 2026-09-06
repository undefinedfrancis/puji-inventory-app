package cn.puji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductCategoriesTest {
    @Test fun groupsProductsByNormalizedCategoryAndKeepsUncategorizedLast() {
        val products = listOf(
            Product(id = 1, name = "牛奶", category = " "),
            Product(id = 2, name = "可乐", category = "饮料"),
            Product(id = 3, name = "面包", category = "食品"),
            Product(id = 4, name = "气泡水", category = "饮料"),
        )

        val groups = ProductCategories.group(products)

        assertEquals(listOf("食品", "饮料", "未分类"), groups.map { it.name })
        assertEquals(listOf(1, 2, 1), groups.map { it.products.size })
        assertEquals(listOf("可乐", "气泡水"), groups[1].products.map { it.name })
    }

    @Test fun countsIncludesAllAndNormalizesBlankCategory() {
        val counts = ProductCategories.counts(
            listOf(
                Product(name = "A", category = "饮料"),
                Product(name = "B", category = ""),
                Product(name = "C", category = "饮料"),
            ),
        )

        assertEquals(
            listOf(
                ProductCategoryCount("全部", 3),
                ProductCategoryCount("饮料", 2),
                ProductCategoryCount("未分类", 1),
            ),
            counts,
        )
    }
}
