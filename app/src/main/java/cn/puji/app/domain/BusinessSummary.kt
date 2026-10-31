package cn.puji.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class TrendPoint(
    val label: String,
    val revenueCents: Long,
    val expenseCents: Long,
)

data class BusinessSummary(
    val salesRevenueCents: Long = 0,
    val purchaseExpenseCents: Long = 0,
    val grossProfitCents: Long = 0,
    val cashBalanceCents: Long = 0,
    val grossMarginBasisPoints: Int = 0,
    val transactionCount: Int = 0,
    val trend: List<TrendPoint> = emptyList(),
    val salesCostCents: Long = 0,
    val writeOffCostCents: Long = 0,
    val stockLossCostCents: Long = 0,
    val valuationDifferenceCents: Long = 0,
) {
    val purchaseSalesDifferenceCents: Long get() = cashBalanceCents
}

object BusinessSummaryCalculator {
    fun calculate(documents: List<StockDocument>): BusinessSummary {
        var salesRevenue=0L; var purchaseExpense=0L; var salesCost=0L
        var writeOff=0L; var stockLoss=0L; var valuation=0L
        documents.forEach { d ->
            val effective=if(d.type==DocumentType.REVERSAL) d.originalType else d.type
            val sign=if(d.type==DocumentType.REVERSAL) -1L else 1L
            val cost=d.lines.fold(0L) { sum,l -> Math.addExact(sum,l.costTotalCents) }
            when(effective) {
                DocumentType.OUTBOUND -> { salesRevenue=Math.addExact(salesRevenue,Math.multiplyExact(d.totalCents,sign));salesCost=Math.addExact(salesCost,Math.multiplyExact(cost,sign)) }
                DocumentType.SALES_RETURN -> { salesRevenue=Math.subtractExact(salesRevenue,Math.multiplyExact(d.totalCents,sign));salesCost=Math.subtractExact(salesCost,Math.multiplyExact(cost,sign)) }
                DocumentType.INBOUND -> purchaseExpense=Math.addExact(purchaseExpense,Math.multiplyExact(d.totalCents,sign))
                DocumentType.PURCHASE_RETURN -> purchaseExpense=Math.subtractExact(purchaseExpense,Math.multiplyExact(d.totalCents,sign))
                DocumentType.WRITE_OFF -> writeOff=Math.addExact(writeOff,Math.multiplyExact(cost,sign))
                DocumentType.STOCK_LOSS -> stockLoss=Math.addExact(stockLoss,Math.multiplyExact(cost,sign))
                else -> Unit
            }
            valuation=Math.addExact(valuation,d.valuationDifferenceCents)
        }
        val profit=Math.subtractExact(salesRevenue,salesCost)
        return BusinessSummary(
            salesRevenueCents=salesRevenue, purchaseExpenseCents=purchaseExpense,
            grossProfitCents=profit, cashBalanceCents=Math.subtractExact(salesRevenue,purchaseExpense),
            grossMarginBasisPoints=if(salesRevenue==0L) 0 else java.math.BigInteger.valueOf(profit)
                .multiply(java.math.BigInteger.valueOf(10_000)).divide(java.math.BigInteger.valueOf(salesRevenue))
                .coerceIn(java.math.BigInteger.valueOf(Int.MIN_VALUE.toLong()),java.math.BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt(),
            transactionCount=documents.size, salesCostCents=salesCost, writeOffCostCents=writeOff,
            stockLossCostCents=stockLoss, valuationDifferenceCents=valuation,
        )
    }

    fun trend(
        documents: List<StockDocument>,
        kind: PeriodKind,
        range: DateRange,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<TrendPoint> = when (kind) {
        PeriodKind.MONTH -> dailyTrend(documents, range, zone)
        PeriodKind.QUARTER, PeriodKind.YEAR -> monthlyTrend(documents, range, zone)
    }

    private fun dailyTrend(
        documents: List<StockDocument>,
        range: DateRange,
        zone: ZoneId,
    ): List<TrendPoint> {
        val grouped = documents
            .asSequence()
            .filter { range.contains(it.createdAt) }
            .groupBy { it.createdAt.toLocalDate(zone) }
        val start = range.startInclusive.toLocalDate(zone)
        val end = range.endExclusive.toLocalDate(zone)
        return generateSequence(start) { current -> current.plusDays(1).takeIf { it.isBefore(end) } }
            .map { date -> grouped.toPoint(date, "${date.dayOfMonth}日") }
            .toList()
    }

    private fun monthlyTrend(
        documents: List<StockDocument>,
        range: DateRange,
        zone: ZoneId,
    ): List<TrendPoint> {
        val grouped = documents
            .asSequence()
            .filter { range.contains(it.createdAt) }
            .groupBy { YearMonth.from(it.createdAt.toLocalDate(zone)) }
        val start = YearMonth.from(range.startInclusive.toLocalDate(zone))
        val end = YearMonth.from(range.endExclusive.toLocalDate(zone))
        return generateSequence(start) { current -> current.plusMonths(1).takeIf { it.isBefore(end) } }
            .map { month -> grouped.toPoint(month, "${month.monthValue}月") }
            .toList()
    }

    private fun <T> Map<T, List<StockDocument>>.toPoint(key: T, label: String): TrendPoint {
        val bucket = this[key].orEmpty()
        return TrendPoint(
            label = label,
            revenueCents = calculate(bucket).salesRevenueCents,
            expenseCents = calculate(bucket).purchaseExpenseCents,
        )
    }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}
