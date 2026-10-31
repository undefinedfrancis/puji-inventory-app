package cn.puji.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.puji.app.domain.DashboardStats
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.Money
import cn.puji.app.domain.Product
import cn.puji.app.domain.StockDocument
import cn.puji.app.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    stats: DashboardStats, lowStock: List<Product>, onInbound: () -> Unit, onOutbound: () -> Unit,
    onInventory: () -> Unit, onAbout: () -> Unit, onReset: () -> Unit,
    todayProfitCents: Long = stats.todayGrossProfitCents, recentDocuments: List<StockDocument> = emptyList(), onOpenLedger: () -> Unit = {},
    onLowStock: () -> Unit = onInventory,
    onOpenDocument: (StockDocument) -> Unit = { onOpenLedger() },
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Column {
                PageHeader("", "今日") { IconButton(onClick = onAbout) { PujiIcon(PujiSymbol.Settings, description = "设置") } }
                Text(SimpleDateFormat("M月d日", Locale.CHINA).format(Date()), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ReceiptCard(Modifier.fillMaxWidth().clickable(onClick = onOpenLedger)) {
                AmountPair("销售额", Money.format(stats.todaySalesCents), "毛利润", Money.format(todayProfitCents), prominent = true)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOutbound, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                    PujiIcon(PujiSymbol.Outbound, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp)); Text("出货")
                }
                OutlinedButton(onClick = onInbound, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                    PujiIcon(PujiSymbol.Inbound, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp)); Text("入货")
                }
            }
        }
        item {
            ReceiptCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onLowStock), verticalAlignment = Alignment.CenterVertically) {
                    Text("待补货", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${stats.lowStockCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PujiIcon(PujiSymbol.ChevronRight)
                }
                if (lowStock.isEmpty()) Text("暂无待补货商品", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                lowStock.take(4).forEach { product ->
                    PujiDivider()
                    Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(onClick = onLowStock),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(product.name)
                            Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("库存 ${product.stockQuantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        PujiIcon(PujiSymbol.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (recentDocuments.isNotEmpty()) item {
            ReceiptCard(Modifier.fillMaxWidth()) {
                Text("最近交易", style = MaterialTheme.typography.titleMedium)
                recentDocuments.take(3).forEach { document ->
                    PujiDivider()
                    Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable { onOpenDocument(document) }, verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PujiIcon(if (document.type == DocumentType.INBOUND) PujiSymbol.Inbound else PujiSymbol.Outbound,
                            tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(documentTypeLabel(document.type))
                            Text(SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(document.createdAt)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.format(document.totalCents), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                        PujiIcon(PujiSymbol.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            ReceiptCard(Modifier.fillMaxWidth().clickable(onClick = onInventory)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("库存总值", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(stats.inventoryValueCents), style = MaterialTheme.typography.headlineSmall)
                    }
                    PujiIcon(PujiSymbol.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${stats.productCount} 种在售商品", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
