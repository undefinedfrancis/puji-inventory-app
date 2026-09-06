package cn.puji.app.domain

data class ProductCategoryGroup(
    val name: String,
    val products: List<Product>,
)

data class ProductCategoryCount(
    val name: String,
    val count: Int,
)

object ProductCategories {
    const val UNCATEGORIZED = "未分类"

    fun normalize(category: String): String = category.trim().ifBlank { UNCATEGORIZED }

    fun group(products: List<Product>): List<ProductCategoryGroup> = products
        .groupBy { normalize(it.category) }
        .toSortedMap(categoryComparator)
        .map { (name, items) -> ProductCategoryGroup(name, items) }

    fun counts(products: List<Product>): List<ProductCategoryCount> = buildList {
        add(ProductCategoryCount("全部", products.size))
        addAll(
            products.groupingBy { normalize(it.category) }
                .eachCount()
                .toSortedMap(categoryComparator)
                .map { (name, count) -> ProductCategoryCount(name, count) },
        )
    }

    private val categoryComparator = Comparator<String> { left, right ->
        when {
            left == right -> 0
            left == UNCATEGORIZED -> 1
            right == UNCATEGORIZED -> -1
            else -> left.compareTo(right)
        }
    }
}
