package cn.puji.app.export

import cn.puji.app.domain.Product
import cn.puji.app.domain.DocumentType
import cn.puji.app.domain.StockDocument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun products(products: List<Product>): String = buildString {
        append('\uFEFF')
        append("名称,条码,分类,进货价,售价,库存,预警值,备注,更新时间\r\n")
        products.forEach { product ->
            appendRow(
                product.name,
                product.barcode,
                product.category,
                "%.2f".format(Locale.US, product.costPriceCents / 100.0),
                "%.2f".format(Locale.US, product.salePriceCents / 100.0),
                product.stockQuantity.toString(),
                product.lowStockThreshold.toString(),
                product.note,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(product.updatedAt)),
            )
        }
    }

    fun documents(documents: List<StockDocument>): String = buildString {
        append('\uFEFF')
        append("单号,类型,时间,商品,条码,单价,数量,小计,单据总额,备注\r\n")
        documents.forEach { document ->
            document.lines.forEach { line ->
                appendRow(
                    document.id.toString(),
                    if (document.type == DocumentType.OUTBOUND) "出货" else "入货",
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(document.createdAt)),
                    line.productName,
                    line.barcode,
                    "%.2f".format(Locale.US, line.unitPriceCents / 100.0),
                    line.quantity.toString(),
                    "%.2f".format(Locale.US, line.lineTotalCents / 100.0),
                    "%.2f".format(Locale.US, document.totalCents / 100.0),
                    document.note,
                )
            }
        }
    }

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }

    private fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
}
