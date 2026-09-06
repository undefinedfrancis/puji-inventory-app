package cn.puji.app.data

import cn.puji.app.domain.BusinessSummary
import cn.puji.app.domain.BusinessSummaryCalculator
import cn.puji.app.domain.DateRange
import cn.puji.app.domain.PeriodKind
import java.time.ZoneId

class BusinessSummaryRepository(private val inventoryRepository: InventoryRepository) {
    fun summary(
        range: DateRange,
        kind: PeriodKind,
        zone: ZoneId = ZoneId.systemDefault(),
    ): BusinessSummary {
        val documents = inventoryRepository.documents(range = range)
        val totals = BusinessSummaryCalculator.calculate(documents)
        return totals.copy(trend = BusinessSummaryCalculator.trend(documents, kind, range, zone))
    }
}
