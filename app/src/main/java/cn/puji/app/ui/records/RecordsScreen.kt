package cn.puji.app.ui.records

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.puji.app.domain.BusinessSummary
import cn.puji.app.domain.DateRange
import cn.puji.app.domain.Money
import cn.puji.app.domain.PeriodKind
import cn.puji.app.ui.components.Metric
import cn.puji.app.ui.components.PageHeader
import cn.puji.app.ui.components.ReceiptCard
import cn.puji.app.ui.theme.Hairline
import cn.puji.app.ui.theme.ScannerOrange
import cn.puji.app.ui.theme.TicketGreen

@Composable
fun RecordsScreen(
    summary: BusinessSummary,
    periodKind: PeriodKind,
    range: DateRange,
    canGoNext: Boolean,
    onPeriodKindChange: (PeriodKind) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenLedger: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageHeader("Business / 经营", "经营记录") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodButton("月度", periodKind == PeriodKind.MONTH, Modifier.weight(1f)) { onPeriodKindChange(PeriodKind.MONTH) }
                PeriodButton("季度", periodKind == PeriodKind.QUARTER, Modifier.weight(1f)) { onPeriodKindChange(PeriodKind.QUARTER) }
                PeriodButton("年度", periodKind == PeriodKind.YEAR, Modifier.weight(1f)) { onPeriodKindChange(PeriodKind.YEAR) }
            }
        }
        item { PeriodNavigator(range.label, canGoNext, onPrevious, onNext) }
        item {
            ReceiptCard(accent = TicketGreen) {
                Text("销售收入", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text(
                    Money.format(summary.salesRevenueCents),
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("共 ${summary.transactionCount} 笔业务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("毛利率 ${formatPercent(summary.grossMarginBasisPoints)}", fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("进货支出", Money.format(summary.purchaseExpenseCents), Modifier.weight(1f))
                    Metric("毛利润", Money.format(summary.grossProfitCents), Modifier.weight(1f), summary.grossProfitCents < 0)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("现金结余", Money.format(summary.cashBalanceCents), Modifier.weight(1f), summary.cashBalanceCents < 0)
                    Metric("交易笔数", "${summary.transactionCount}", Modifier.weight(1f))
                }
            }
        }
        item {
            ReceiptCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("周期趋势", fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Legend(TicketGreen, "收入")
                        Legend(ScannerOrange, "支出")
                    }
                }
                if (summary.trend.none { it.revenueCents != 0L || it.expenseCents != 0L }) {
                    Text(
                        "本周期还没有流水",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    TrendChart(summary)
                }
            }
        }
        item {
            Button(onClick = onOpenLedger, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text("查阅流水与导出")
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

@Composable
private fun PeriodButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(text) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
}

@Composable
fun PeriodNavigator(label: String, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPrevious, modifier = Modifier.width(52.dp), contentPadding = PaddingValues(0.dp)) { Text("‹", fontSize = 24.sp) }
        Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
        OutlinedButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.width(52.dp), contentPadding = PaddingValues(0.dp)) { Text("›", fontSize = 24.sp) }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("●", color = color, fontSize = 10.sp)
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendChart(summary: BusinessSummary) {
    val points = summary.trend
    val grid = Hairline
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val maxValue = points.maxOfOrNull { maxOf(it.revenueCents, it.expenseCents) }?.coerceAtLeast(1L) ?: 1L
        val baseline = size.height - 8.dp.toPx()
        drawLine(grid, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())
        val slot = size.width / points.size.coerceAtLeast(1)
        val barWidth = (slot * .28f).coerceAtMost(8.dp.toPx())
        points.forEachIndexed { index, point ->
            val center = slot * index + slot / 2f
            val revenueHeight = (point.revenueCents.toFloat() / maxValue) * (baseline - 8.dp.toPx())
            val expenseHeight = (point.expenseCents.toFloat() / maxValue) * (baseline - 8.dp.toPx())
            drawRect(TicketGreen, Offset(center - barWidth, baseline - revenueHeight), Size(barWidth, revenueHeight))
            drawRect(ScannerOrange, Offset(center + 1.dp.toPx(), baseline - expenseHeight), Size(barWidth, expenseHeight))
        }
    }
}

private fun formatPercent(basisPoints: Int): String = "%.2f%%".format(basisPoints / 100.0)
