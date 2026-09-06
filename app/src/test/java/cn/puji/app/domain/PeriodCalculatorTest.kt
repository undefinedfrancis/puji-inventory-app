package cn.puji.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test fun month_startsAtFirstDayAndEndsAtNextMonth() {
        val range = PeriodCalculator.range(PeriodKind.MONTH, LocalDate.of(2026, 9, 6), zone)

        assertEquals("2026 年 9 月", range.label)
        assertEquals(LocalDate.of(2026, 9, 1), localDate(range.startInclusive))
        assertEquals(LocalDate.of(2026, 10, 1), localDate(range.endExclusive))
    }

    @Test fun quarter_usesNaturalQuarter() {
        val range = PeriodCalculator.range(PeriodKind.QUARTER, LocalDate.of(2026, 9, 6), zone)

        assertEquals("2026 年第 3 季度", range.label)
        assertEquals(LocalDate.of(2026, 7, 1), localDate(range.startInclusive))
        assertEquals(LocalDate.of(2026, 10, 1), localDate(range.endExclusive))
    }

    @Test fun year_usesCalendarYear() {
        val range = PeriodCalculator.range(PeriodKind.YEAR, LocalDate.of(2026, 9, 6), zone)

        assertEquals("2026 年", range.label)
        assertEquals(LocalDate.of(2026, 1, 1), localDate(range.startInclusive))
        assertEquals(LocalDate.of(2027, 1, 1), localDate(range.endExclusive))
    }

    @Test fun leapYearFebruary_hasTwentyNineDays() {
        val range = PeriodCalculator.range(PeriodKind.MONTH, LocalDate.of(2024, 2, 16), zone)

        assertTrue(range.contains(atStart(LocalDate.of(2024, 2, 29))))
        assertFalse(range.contains(atStart(LocalDate.of(2024, 3, 1))))
    }

    @Test fun custom_includesWholeEndDateAndRejectsReverse() {
        val range = PeriodCalculator.custom(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 9, 6),
            zone,
        ).getOrThrow()

        assertEquals("2026-07-01 至 2026-09-06", range.label)
        assertEquals(LocalDate.of(2026, 9, 7), localDate(range.endExclusive))
        assertTrue(
            PeriodCalculator.custom(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 6),
                zone,
            ).isFailure,
        )
    }

    @Test fun previousAndNext_moveBySelectedPeriod() {
        val anchor = LocalDate.of(2026, 9, 6)

        assertEquals(LocalDate.of(2026, 8, 6), PeriodCalculator.previous(PeriodKind.MONTH, anchor))
        assertEquals(LocalDate.of(2026, 12, 6), PeriodCalculator.next(PeriodKind.QUARTER, anchor))
        assertEquals(LocalDate.of(2027, 9, 6), PeriodCalculator.next(PeriodKind.YEAR, anchor))
    }

    @Test fun isCurrent_comparesWholePeriod() {
        val today = LocalDate.of(2026, 9, 6)

        assertTrue(PeriodCalculator.isCurrent(PeriodKind.MONTH, LocalDate.of(2026, 9, 30), today))
        assertFalse(PeriodCalculator.isCurrent(PeriodKind.MONTH, LocalDate.of(2026, 8, 31), today))
        assertTrue(PeriodCalculator.isCurrent(PeriodKind.QUARTER, LocalDate.of(2026, 7, 1), today))
    }

    private fun localDate(value: Long): LocalDate = Instant.ofEpochMilli(value).atZone(zone).toLocalDate()

    private fun atStart(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
}
