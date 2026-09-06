package cn.puji.app.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class PeriodKind { MONTH, QUARTER, YEAR }

data class DateRange(
    val startInclusive: Long,
    val endExclusive: Long,
    val label: String,
) {
    init {
        require(startInclusive < endExclusive) { "开始时间必须早于结束时间" }
    }

    fun contains(timestamp: Long): Boolean = timestamp in startInclusive until endExclusive
}

object PeriodCalculator {
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun range(
        kind: PeriodKind,
        anchor: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DateRange {
        val start = periodStart(kind, anchor)
        val end = when (kind) {
            PeriodKind.MONTH -> start.plusMonths(1)
            PeriodKind.QUARTER -> start.plusMonths(3)
            PeriodKind.YEAR -> start.plusYears(1)
        }
        val label = when (kind) {
            PeriodKind.MONTH -> "${start.year} 年 ${start.monthValue} 月"
            PeriodKind.QUARTER -> "${start.year} 年第 ${((start.monthValue - 1) / 3) + 1} 季度"
            PeriodKind.YEAR -> "${start.year} 年"
        }
        return DateRange(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.atStartOfDay(zone).toInstant().toEpochMilli(), label)
    }

    fun custom(
        start: LocalDate,
        endInclusive: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Result<DateRange> = runCatching {
        require(!start.isAfter(endInclusive)) { "开始日期不能晚于结束日期" }
        DateRange(
            startInclusive = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            endExclusive = endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            label = "${start.format(isoDate)} 至 ${endInclusive.format(isoDate)}",
        )
    }

    fun previous(kind: PeriodKind, anchor: LocalDate): LocalDate = when (kind) {
        PeriodKind.MONTH -> anchor.minusMonths(1)
        PeriodKind.QUARTER -> anchor.minusMonths(3)
        PeriodKind.YEAR -> anchor.minusYears(1)
    }

    fun next(kind: PeriodKind, anchor: LocalDate): LocalDate = when (kind) {
        PeriodKind.MONTH -> anchor.plusMonths(1)
        PeriodKind.QUARTER -> anchor.plusMonths(3)
        PeriodKind.YEAR -> anchor.plusYears(1)
    }

    fun isCurrent(
        kind: PeriodKind,
        anchor: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Boolean = periodStart(kind, anchor) == periodStart(kind, today)

    private fun periodStart(kind: PeriodKind, anchor: LocalDate): LocalDate = when (kind) {
        PeriodKind.MONTH -> anchor.withDayOfMonth(1)
        PeriodKind.QUARTER -> LocalDate.of(anchor.year, ((anchor.monthValue - 1) / 3) * 3 + 1, 1)
        PeriodKind.YEAR -> LocalDate.of(anchor.year, 1, 1)
    }
}
