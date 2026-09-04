package cn.puji.app.export

import cn.puji.app.domain.Product
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

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }

    private fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
}
