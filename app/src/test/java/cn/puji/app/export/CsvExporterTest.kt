package cn.puji.app.export

import cn.puji.app.domain.Product
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
}
