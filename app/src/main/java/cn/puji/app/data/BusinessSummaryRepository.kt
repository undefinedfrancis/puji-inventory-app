package cn.puji.app.data

import cn.puji.app.domain.*
import java.time.ZoneId

class BusinessSummaryRepository(private val inventoryRepository: InventoryRepository) {
    fun summary(range:DateRange,kind:PeriodKind,zone:ZoneId=ZoneId.systemDefault()):BusinessSummary {
        val db=inventoryRepository.database.readableDatabase
        val args=arrayOf(range.startInclusive.toString(),range.endExclusive.toString())
        var count=0
        // Aggregate line costs once per document; joining raw lines would repeat header totals.
        val grouped=db.rawQuery("""SELECT d.type,d.original_type,SUM(d.total_cents) AS amount,
            COALESCE(SUM(c.cost),0) AS cost,SUM(d.valuation_difference_cents) AS variance,COUNT(*) AS n
            FROM documents d LEFT JOIN (SELECT l.document_id,SUM(l.line_cost_cents) AS cost FROM document_lines l
                WHERE l.document_id IN (SELECT id FROM documents WHERE created_at>=? AND created_at<?) GROUP BY l.document_id) c ON c.document_id=d.id
            WHERE d.created_at>=? AND d.created_at<? GROUP BY d.type,d.original_type""",args+args).use { c -> buildList {
            while(c.moveToNext()) {
                count=Math.addExact(count,c.getInt(5))
                add(StockDocument(id=0,type=DocumentType.valueOf(c.getString(0)),totalCents=c.getLong(2),note="",createdAt=0,
                    originalType=c.getString(1)?.let(DocumentType::valueOf),valuationDifferenceCents=c.getLong(4),
                    lines=listOf(DocumentLine(productId=0,productName="",barcode="",unitPriceCents=0,quantity=1,lineCostCents=c.getLong(3)))))
            }
        } }
        // Trends require only timestamp, effective type and business amount, never product snapshots.
        val trendHeaders=db.rawQuery("SELECT id,type,total_cents,created_at,original_type FROM documents WHERE created_at>=? AND created_at<?",args).use { c -> buildList {
            while(c.moveToNext()) add(StockDocument(c.getLong(0),DocumentType.valueOf(c.getString(1)),c.getLong(2),"",c.getLong(3),originalType=c.getString(4)?.let(DocumentType::valueOf)))
        } }
        return BusinessSummaryCalculator.calculate(grouped).copy(transactionCount=count,trend=BusinessSummaryCalculator.trend(trendHeaders,kind,range,zone))
    }
}
