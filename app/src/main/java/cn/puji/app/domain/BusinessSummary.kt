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
)

object BusinessSummaryCalculator {
    fun calculate(documents: List<StockDocument>): BusinessSummary {
        val salesRevenue = documents.filter { it.type == DocumentType.OUTBOUND }.sumOf { it.totalCents }
        val purchaseExpense = documents.filter { it.type == DocumentType.INBOUND }.sumOf { it.totalCents }
        val grossProfit = documents
            .filter { it.type == DocumentType.OUTBOUND }
            .sumOf { document -> document.lines.sumOf(DocumentLine::grossProfitCents) }
        return BusinessSummary(
            salesRevenueCents = salesRevenue,
            purchaseExpenseCents = purchaseExpense,
            grossProfitCents = grossProfit,
            cashBalanceCents = salesRevenue - purchaseExpense,
            grossMarginBasisPoints = if (salesRevenue == 0L) 0 else (grossProfit * 10_000L / salesRevenue).toInt(),
            transactionCount = documents.size,
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
            revenueCents = bucket.filter { it.type == DocumentType.OUTBOUND }.sumOf { it.totalCents },
            expenseCents = bucket.filter { it.type == DocumentType.INBOUND }.sumOf { it.totalCents },
        )
    }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}
