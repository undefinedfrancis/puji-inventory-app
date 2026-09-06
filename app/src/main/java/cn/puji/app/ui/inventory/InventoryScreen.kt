package cn.puji.app.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.puji.app.domain.Money
import cn.puji.app.domain.Product
import cn.puji.app.domain.ProductCategories
import cn.puji.app.domain.ProductCategoryCount
import cn.puji.app.ui.components.EmptyState
import cn.puji.app.ui.components.PageHeader
import cn.puji.app.ui.components.ReceiptCard
import cn.puji.app.ui.components.StatusPill

@Composable
fun InventoryScreen(
    products: List<Product>,
    query: String,
    categories: List<ProductCategoryCount>,
    selectedCategory: String,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Product) -> Unit,
    onExport: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        PageHeader(
            kicker = "Inventory / 库存",
            title = "货架总览",
            action = { OutlinedButton(onClick = onExport) { Text("导出") } },
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("搜索名称、条码或分类") },
            leadingIcon = { Text("⌕", fontSize = 24.sp) },
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.name }) { category ->
                FilterButton(
                    text = "${category.name} ${category.count}",
                    selected = selectedCategory == category.name,
                    onClick = { onCategoryChange(category.name) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("＋ 新增商品") }
        Spacer(Modifier.height(14.dp))
        if (products.isEmpty()) {
            EmptyState("□", "没有找到商品", "换个关键词，或者新增第一件商品")
        } else {
            val groups = if (selectedCategory == "全部") ProductCategories.group(products)
            else listOf(cn.puji.app.domain.ProductCategoryGroup(selectedCategory, products))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEach { group ->
                    item(key = "group-${group.name}") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(group.name, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("${group.products.size} 件", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                    items(group.products, key = { it.id }) { product -> ProductCard(product, onEdit) }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun FilterButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(text) }
    else OutlinedButton(onClick = onClick) { Text(text) }
}

@Composable
private fun ProductCard(product: Product, onEdit: (Product) -> Unit) {
    ReceiptCard(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(product) },
        accent = if (product.isLowStock) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(
                    listOf(ProductCategories.normalize(product.category), product.barcode.ifBlank { "无条码" }).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            StatusPill(if (product.isLowStock) "需补货" else "库存正常", product.isLowStock)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("售价", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(Money.format(product.salePriceCents), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("进价", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(Money.format(product.costPriceCents), fontFamily = FontFamily.Monospace)
            }
            Column {
                Text("现有库存", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("${product.stockQuantity}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
            }
        }
    }
}
