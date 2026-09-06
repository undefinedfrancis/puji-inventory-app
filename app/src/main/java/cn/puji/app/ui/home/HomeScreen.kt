package cn.puji.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.puji.app.domain.DashboardStats
import cn.puji.app.domain.Money
import cn.puji.app.domain.Product
import cn.puji.app.ui.components.Metric
import cn.puji.app.ui.components.PageHeader
import cn.puji.app.ui.components.ReceiptCard
import cn.puji.app.ui.components.StatusPill
import cn.puji.app.ui.theme.ScannerOrange
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    stats: DashboardStats,
    lowStock: List<Product>,
    onInbound: () -> Unit,
    onOutbound: () -> Unit,
    onInventory: () -> Unit,
    onAbout: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(
                kicker = SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date()),
                title = "柜台早上好",
                action = { OutlinedButton(onClick = onAbout) { Text("关于") } },
            )
        }
        item {
            ReceiptCard(accent = ScannerOrange) {
                Text("今日出货", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text(
                    Money.format(stats.todaySalesCents),
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Text("${stats.todayItemCount} 件商品已出库", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOutbound,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerOrange),
                    ) { Text("扫码出货") }
                    OutlinedButton(onClick = onInbound, modifier = Modifier.weight(1f)) { Text("登记入货") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("在售商品", "${stats.productCount}", Modifier.weight(1f))
                Metric("库存预警", "${stats.lowStockCount}", Modifier.weight(1f), highlight = stats.lowStockCount > 0)
            }
        }
        item {
            ReceiptCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("库存成本", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(Money.format(stats.inventoryValueCents), fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(onClick = onInventory) { Text("查看库存") }
                }
            }
        }
        item {
            Text("需要补货", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            if (lowStock.isEmpty()) {
                Text("库存都很充足", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    lowStock.take(4).forEach { product ->
                        ReceiptCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text(product.category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                StatusPill("剩 ${product.stockQuantity}", warning = true)
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(14.dp))
            ReceiptCard(accent = MaterialTheme.colorScheme.surfaceVariant) {
                Text("数据管理", fontWeight = FontWeight.Black)
                Text(
                    "仅在需要从空白账本重新开始时使用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E)),
                ) { Text("重置全部数据") }
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}
