package cn.puji.app.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessSummaryCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test fun calculate_reportsBothProfitDefinitions() {
        val documents = listOf(
            document(DocumentType.OUTBOUND, total = 1_200, unitPrice = 600, costPrice = 400, quantity = 2),
            document(DocumentType.INBOUND, total = 500, unitPrice = 500, costPrice = 500),
            document(DocumentType.OUTBOUND, total = 0, unitPrice = 0, costPrice = 0),
        )

        val summary = BusinessSummaryCalculator.calculate(documents)

        assertEquals(1_200, summary.salesRevenueCents)
        assertEquals(500, summary.purchaseExpenseCents)
        assertEquals(400, summary.grossProfitCents)
        assertEquals(700, summary.cashBalanceCents)
        assertEquals(3333, summary.grossMarginBasisPoints)
        assertEquals(3, summary.transactionCount)
    }

    @Test fun calculate_handlesNoSalesAndNegativeProfit() {
        val noSales = BusinessSummaryCalculator.calculate(listOf(document(DocumentType.INBOUND, 500, 500, 500)))
        val loss = BusinessSummaryCalculator.calculate(listOf(document(DocumentType.OUTBOUND, 100, 100, 150)))

        assertEquals(0, noSales.grossMarginBasisPoints)
        assertEquals(-50, loss.grossProfitCents)
        assertEquals(-5000, loss.grossMarginBasisPoints)
    }

    @Test fun trend_monthFillsEveryDayIncludingEmptyDays() {
        val range = PeriodCalculator.range(PeriodKind.MONTH, LocalDate.of(2026, 9, 1), zone)
        val documents = listOf(
            document(DocumentType.OUTBOUND, 200, 200, 100, createdAt = atStart(LocalDate.of(2026, 9, 2))),
            document(DocumentType.INBOUND, 80, 80, 80, createdAt = atStart(LocalDate.of(2026, 9, 2))),
        )

        val trend = BusinessSummaryCalculator.trend(documents, PeriodKind.MONTH, range, zone)

        assertEquals(30, trend.size)
        assertEquals(TrendPoint("1日", 0, 0), trend.first())
        assertEquals(TrendPoint("2日", 200, 80), trend[1])
    }

    @Test fun trend_quarterAndYearUseMonthlyBuckets() {
        val quarter = PeriodCalculator.range(PeriodKind.QUARTER, LocalDate.of(2026, 8, 1), zone)
        val year = PeriodCalculator.range(PeriodKind.YEAR, LocalDate.of(2026, 8, 1), zone)

        val quarterTrend = BusinessSummaryCalculator.trend(emptyList(), PeriodKind.QUARTER, quarter, zone)
        val yearTrend = BusinessSummaryCalculator.trend(emptyList(), PeriodKind.YEAR, year, zone)

        assertEquals(listOf("7月", "8月", "9月"), quarterTrend.map { it.label })
        assertEquals(12, yearTrend.size)
        assertEquals("12月", yearTrend.last().label)
    }

    private fun document(
        type: DocumentType,
        total: Long,
        unitPrice: Long,
        costPrice: Long,
        quantity: Int = 1,
        createdAt: Long = 0,
    ) = StockDocument(
        id = createdAt + type.ordinal + 1,
        type = type,
        totalCents = total,
        note = "",
        createdAt = createdAt,
        lines = listOf(
            DocumentLine(
                productId = 1,
                productName = "商品",
                barcode = "",
                unitPriceCents = unitPrice,
                costPriceSnapshotCents = costPrice,
                quantity = quantity,
            ),
        ),
    )

    private fun atStart(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
}
