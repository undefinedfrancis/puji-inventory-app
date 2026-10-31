package cn.puji.app.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.puji.app.domain.Money
import cn.puji.app.domain.Product
import cn.puji.app.domain.ProductCategories
import cn.puji.app.domain.ProductCategoryCount
import cn.puji.app.ui.components.*

@Composable
fun InventoryScreen(
    products: List<Product>, query: String, categories: List<ProductCategoryCount>, selectedCategory: String,
    onQueryChange: (String) -> Unit, onCategoryChange: (String) -> Unit, onAdd: () -> Unit,
    onEdit: (Product) -> Unit, onExport: () -> Unit, onManage: ((Product) -> Unit)? = null,
    initialStockFilter: String = "全部",
) {
    var stockFilter by remember(initialStockFilter) { mutableStateOf(initialStockFilter) }
    val visible = products.filter { when(stockFilter) { "低库存" -> it.isLowStock; "停用" -> !it.active; else -> it.active } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader("", "商品") {
            TextButton(onClick = onExport) { Text("导出") }
            IconButton(onClick = onAdd) { PujiIcon(PujiSymbol.Plus, description = "新增商品") }
        } }
        item { OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = MaterialTheme.shapes.medium, label = { Text("搜索商品") },
            leadingIcon = { PujiIcon(PujiSymbol.Search, tint = MaterialTheme.colorScheme.onSurfaceVariant) }) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(categories, key = { it.name }) { category -> ChoiceButton(category.name,
                selectedCategory == category.name, onClick = { onCategoryChange(category.name) }) }
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("全部", "低库存", "停用").forEach { name -> ChoiceButton(name, stockFilter == name, onClick = { stockFilter = name }) }
        } }
        if (visible.isEmpty()) item { EmptyState("", "暂无商品", if (query.isBlank()) "添加商品开始记账" else "试试其他名称或条码") }
        items(visible, key = { it.id }) { product ->
            Column {
                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { (onManage ?: onEdit)(product) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(product.name)
                        Text(ProductCategories.normalize(product.category), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(Money.format(product.salePriceCents), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                        Text(if (product.active) "库存 ${product.stockQuantity}" else "已停用 · ${product.stockQuantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (product.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PujiIcon(PujiSymbol.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PujiDivider()
            }
        }
    }
}
