package cn.puji.app.export

import cn.puji.app.domain.Product
import cn.puji.app.domain.DocumentLine
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.StockDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test fun products_escapesChineseCommaQuoteAndNewline() {
        val csv = CsvExporter.products(
            listOf(Product(id = 1, name = "可乐", note = "促销,\"红\"\n第二行", salePriceCents = 350)),
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"促销,\"\"红\"\"\n第二行\""))
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test fun documents_exportsOneRowPerLineWithDocumentSnapshot() {
        val csv = CsvExporter.documents(
            listOf(
                StockDocument(
                    id = 12,
                    type = DocumentType.OUTBOUND,
                    totalCents = 700,
                    note = "团购,已付",
                    createdAt = 0,
                    lines = listOf(
                        DocumentLine(
                            documentId = 12,
                            productId = 1,
                            productName = "可乐",
                            barcode = "6901",
                            unitPriceCents = 350,
                            quantity = 2,
                        ),
                    ),
                ),
            ),
        )

        assertTrue(csv.startsWith("\uFEFF单号,类型,时间,商品,条码,单价,数量,小计,单据总额,备注\r\n"))
        assertTrue(csv.contains("12,出货,1970-01-01 08:00,可乐,6901,3.50,2,7.00,7.00,\"团购,已付\""))
        assertEquals(3, csv.split("\r\n").size)
    }
}
