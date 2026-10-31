package cn.puji.app.ui.records

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.puji.app.domain.BusinessSummary
import cn.puji.app.domain.DateRange
import cn.puji.app.domain.Money
import cn.puji.app.domain.PeriodKind
import cn.puji.app.ui.components.*

@Composable
fun RecordsScreen(summary: BusinessSummary, periodKind: PeriodKind, range: DateRange, canGoNext: Boolean,
    onPeriodKindChange: (PeriodKind) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onOpenLedger: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { PageHeader("", "经营") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceButton("月", periodKind == PeriodKind.MONTH, Modifier.weight(1f)) { onPeriodKindChange(PeriodKind.MONTH) }
            ChoiceButton("季", periodKind == PeriodKind.QUARTER, Modifier.weight(1f)) { onPeriodKindChange(PeriodKind.QUARTER) }
            ChoiceButton("年", periodKind == PeriodKind.YEAR, Modifier.weight(1f)) { onPeriodKindChange(PeriodKind.YEAR) }
        } }
        item { PeriodNavigator(range.label, canGoNext, onPrevious, onNext) }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            AmountPair("销售额", Money.format(summary.salesRevenueCents), "进货额", Money.format(summary.purchaseExpenseCents))
        } }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Legend(MaterialTheme.colorScheme.primary, "销售")
                Spacer(Modifier.width(16.dp)); Legend(MaterialTheme.colorScheme.secondary, "进货")
            }
            if (summary.trend.none { it.revenueCents != 0L || it.expenseCents != 0L }) {
                EmptyState("", "本周期暂无交易", "")
            } else TrendChart(summary)
        } }
        item { ReceiptCard(Modifier.fillMaxWidth()) {
            SummaryRow("毛利润", Money.format(summary.grossProfitCents), summary.grossProfitCents < 0)
            PujiDivider()
            SummaryRow("毛利率", "%.2f%%".format(summary.grossMarginBasisPoints / 100.0))
            PujiDivider()
            SummaryRow("进销差额", Money.format(summary.cashBalanceCents), summary.cashBalanceCents < 0)
            PujiDivider()
            SummaryRow("交易笔数", "${summary.transactionCount}")
            if(summary.writeOffCostCents != 0L) { PujiDivider(); SummaryRow("报损成本", Money.format(summary.writeOffCostCents)) }
            if(summary.stockLossCostCents != 0L) { PujiDivider(); SummaryRow("盘亏成本", Money.format(summary.stockLossCostCents)) }
            if(summary.valuationDifferenceCents != 0L) { PujiDivider(); SummaryRow("库存估值差额", Money.format(summary.valuationDifferenceCents)) }
            PujiDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onOpenLedger), verticalAlignment = Alignment.CenterVertically) {
                Text("流水", modifier = Modifier.weight(1f))
                PujiIcon(PujiSymbol.ChevronRight, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, negative: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PeriodNavigator(label: String, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) { PujiIcon(PujiSymbol.ChevronLeft, description = "上一周期") }
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = onNext, enabled = canGoNext) { PujiIcon(PujiSymbol.ChevronRight, description = "下一周期",
            tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)) }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendChart(summary: BusinessSummary) {
    val points = summary.trend
    val grid = MaterialTheme.colorScheme.outlineVariant
    val revenue = MaterialTheme.colorScheme.primary
    val expense = MaterialTheme.colorScheme.secondary
    val maxValue = points.maxOfOrNull { maxOf(it.revenueCents, it.expenseCents) }?.coerceAtLeast(1L) ?: 1L
    val minValue = points.minOfOrNull { minOf(0L, it.revenueCents, it.expenseCents) } ?: 0L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if(minValue < 0) "${Money.format(minValue)} 至 ${Money.format(maxValue)}" else "最高 ${Money.format(maxValue)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(160.dp).semantics {
            contentDescription = "销售与进货趋势。" + points.joinToString("；") { "${it.label}销售${Money.format(it.revenueCents)}，进货${Money.format(it.expenseCents)}" }
        }) {
            val inset = 5.dp.toPx()
            val plotHeight = size.height - inset * 2
            repeat(4) { i -> val y=inset+plotHeight*i/3f; drawLine(grid,Offset(inset,y),Offset(size.width-inset,y),1.dp.toPx()) }
            fun drawSeries(color: Color, value: (Int) -> Long) {
                val path = Path()
                points.indices.forEach { i ->
                    val x = if(points.size == 1) size.width/2 else inset+(size.width-inset*2)*i/(points.size-1)
                    val fraction = (value(i).toDouble() - minValue.toDouble()) / (maxValue.toDouble() - minValue.toDouble())
                    val y = inset+plotHeight*(1f-fraction.toFloat())
                    if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
                    if(points.size <= 12 || i==0 || i==points.lastIndex) drawCircle(color,3.dp.toPx(),Offset(x,y))
                }
                drawPath(path,color,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
            }
            drawSeries(revenue) { points[it].revenueCents }
            drawSeries(expense) { points[it].expenseCents }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(points.first().label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if(points.size > 2) Text(points[points.size/2].label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if(points.size > 1) Text(points.last().label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
