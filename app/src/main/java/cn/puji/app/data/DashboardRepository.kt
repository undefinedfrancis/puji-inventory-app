package cn.puji.app.data

import cn.puji.app.domain.DashboardStats
import java.util.Calendar

class DashboardRepository(private val database: PujiDatabase) {
    fun stats(now: Long = System.currentTimeMillis()): DashboardStats {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val db = database.readableDatabase
        val sale = db.rawQuery(
            """SELECT COALESCE(SUM(d.total_cents),0), COALESCE(SUM(l.quantity),0)
               FROM documents d LEFT JOIN document_lines l ON l.document_id=d.id
               WHERE d.type='OUTBOUND' AND d.created_at>=?""".trimIndent(),
            arrayOf(start.toString()),
        ).use { it.moveToFirst(); it.getLong(0) to it.getInt(1) }
        val products = db.rawQuery(
            """SELECT COUNT(*),
               COALESCE(SUM(CASE WHEN stock_quantity<=low_stock_threshold THEN 1 ELSE 0 END),0),
               COALESCE(SUM(stock_quantity*cost_price_cents),0)
               FROM products WHERE active=1""".trimIndent(), null,
        ).use { it.moveToFirst(); Triple(it.getInt(0), it.getInt(1), it.getLong(2)) }
        return DashboardStats(sale.first, sale.second, products.first, products.second, products.third)
    }
}
