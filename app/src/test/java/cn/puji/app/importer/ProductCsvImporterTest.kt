package cn.puji.app.importer

import org.junit.Assert.*
import org.junit.Test

class ProductCsvImporterTest {
    private val header = "名称,条码,分类,进货价,售价,库存,预警值,备注,更新时间\r\n"

    @Test fun parsesExportBomQuotedCommaAndMultiline() {
        val preview = ProductCsvImporter.preview("\uFEFF" + header + "\"可乐,大瓶\",001,饮料,2.20,3.50,18,5,\"促销\"\"价\n次行\",2026-10-31\r\n")
        assertTrue(preview.errors.toString(), preview.errors.isEmpty())
        assertEquals("001", preview.rows.single().product.barcode)
        assertEquals("可乐,大瓶", preview.rows.single().product.name)
        assertEquals("促销\"价\n次行", preview.rows.single().product.note)
        assertEquals(220L, preview.rows.single().product.costPriceCents)
    }

    @Test fun reportsEveryDuplicateBarcodeLineIncludingFirst() {
        val preview = ProductCsvImporter.preview(header + "可乐,001,饮料,2,3,0,5,,\r\n雪碧,001,饮料,2,3,0,5,,\r\n")
        assertEquals(setOf(2, 3), preview.errors.map { it.row }.toSet())
    }

    @Test fun emptyBarcodesRemainDistinct() {
        val preview = ProductCsvImporter.preview(header + "可乐,,饮料,2,3,0,5,,\r\n雪碧,,饮料,2,3,0,5,,\r\n")
        assertTrue(preview.errors.isEmpty())
        assertEquals(2, preview.rows.size)
    }

    @Test fun rejectsLedgerCsvAndInvalidNumbersAndUnclosedQuotes() {
        assertTrue(ProductCsvImporter.preview("单号,类型,时间,商品,条码,单价,数量\n1,出货,今天,可乐,001,3,1").errors.isNotEmpty())
        for (price in listOf("-1", "NaN", "1.001", "99999999999999999999999", "1e2")) {
            assertTrue(price, ProductCsvImporter.preview(header + "可乐,001,饮料,$price,3,0,5,,").errors.isNotEmpty())
        }
        assertTrue(ProductCsvImporter.preview(header + "\"未闭合").errors.isNotEmpty())
    }

    @Test fun templateCanBePreviewedAndHasNoExampleProduct() {
        val preview = ProductCsvImporter.preview(ProductCsvImporter.template())
        assertTrue(preview.errors.isEmpty())
        assertTrue(preview.rows.isEmpty())
    }
}
