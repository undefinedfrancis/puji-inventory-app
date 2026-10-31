package cn.puji.app.data

import cn.puji.app.domain.*
import java.time.LocalDate
import java.time.ZoneId

class DashboardRepository(private val database:PujiDatabase) {
    fun stats(now:Long=System.currentTimeMillis()):DashboardStats {
        val zone=ZoneId.systemDefault()
        val today=java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start=today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end=today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val db=database.readableDatabase
        val summary=BusinessSummaryRepository(InventoryRepository(database)).summary(DateRange(start,end,"今日"),PeriodKind.MONTH,zone)
        val itemCount=db.rawQuery("""SELECT COALESCE(SUM(CASE
            WHEN d.type='OUTBOUND' THEN l.quantity WHEN d.type='SALES_RETURN' THEN -l.quantity
            WHEN d.type='REVERSAL' AND d.original_type='OUTBOUND' THEN -l.quantity
            WHEN d.type='REVERSAL' AND d.original_type='SALES_RETURN' THEN l.quantity ELSE 0 END),0)
            FROM document_lines l JOIN documents d ON d.id=l.document_id WHERE d.created_at>=? AND d.created_at<?""",arrayOf(start.toString(),end.toString())).use { it.moveToFirst();it.getInt(0) }
        val products=db.rawQuery("""SELECT COALESCE(SUM(CASE WHEN active=1 THEN 1 ELSE 0 END),0),
            COALESCE(SUM(CASE WHEN active=1 AND stock_quantity<=low_stock_threshold THEN 1 ELSE 0 END),0),
            COALESCE(SUM(stock_cost_cents),0) FROM products""",null).use { it.moveToFirst();Triple(it.getInt(0),it.getInt(1),it.getLong(2)) }
        return DashboardStats(summary.salesRevenueCents,itemCount,products.first,products.second,products.third,summary.grossProfitCents)
    }
}
