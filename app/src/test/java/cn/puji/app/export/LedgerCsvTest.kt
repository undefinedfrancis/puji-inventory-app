package cn.puji.app.export

import cn.puji.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class LedgerCsvTest {
    @Test fun salesReturnIsNotMislabeledAsPurchase() {
        val line = DocumentLine(productId = 1, productName = "商品", barcode = "", unitPriceCents = 200, quantity = 1, lineCostCents = 101)
        val csv = CsvExporter.documents(listOf(StockDocument(1, DocumentType.SALES_RETURN, 200, "", 0, listOf(line))))
        assertTrue(csv.contains("1,销售退货,"))
        assertTrue(csv.contains("2.00,1,-2.00,-1.01,-0.99"))
    }

    @Test fun zeroDifferenceCountStillExportsItsDocument() {
        val csv = CsvExporter.documents(listOf(StockDocument(9, DocumentType.STOCK_GAIN, 0, "实点一致", 0, expectedQuantity = 5, actualQuantity = 5)))
        assertTrue(csv.contains("9,盘盈,"))
        assertTrue(csv.contains("实点一致"))
    }
}
