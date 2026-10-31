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
                decimal(product.costPriceCents),
                decimal(product.salePriceCents),
                product.stockQuantity.toString(),
                product.lowStockThreshold.toString(),
                product.note,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(product.updatedAt)),
            )
        }
    }

    fun documents(documents: List<StockDocument>): String = buildString {
        append('\uFEFF')
        append("单号,类型,时间,商品,条码,单价,数量,小计,成本金额,毛利润,单据总额,备注,原单号,原单类型,库存变动,库存成本变动,估值差额,成本来源\r\n")
        documents.forEach { document ->
            val effective = if(document.type == DocumentType.REVERSAL) document.originalType else document.type
            val sign = when(effective) {
                DocumentType.INBOUND, DocumentType.OUTBOUND -> 1L
                DocumentType.SALES_RETURN, DocumentType.PURCHASE_RETURN -> -1L
                else -> 0L
            } * if(document.type == DocumentType.REVERSAL) -1L else 1L
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(document.createdAt))
            // A zero-difference count has a header but no lines and must remain visible in exports.
            if(document.lines.isEmpty()) appendRow(document.id.toString(), label(document.type), time, "", "", "", "", "0.00", "0.00", "",
                decimal(Math.multiplyExact(document.totalCents, sign)), document.note, document.originalDocumentId?.toString().orEmpty(),
                document.originalType?.let(::label).orEmpty(), "0", "0.00", decimal(document.valuationDifferenceCents), document.costSource)
            document.lines.forEach { line ->
                val costSign = if(sign == 0L) 1L else sign
                val profit = if(effective == DocumentType.OUTBOUND || effective == DocumentType.SALES_RETURN)
                    decimal(Math.multiplyExact(line.grossProfitCents, sign)) else ""
                appendRow(
                    document.id.toString(),
                    label(document.type),
                    time,
                    line.productName,
                    line.barcode,
                    decimal(line.unitPriceCents),
                    line.quantity.toString(),
                    decimal(Math.multiplyExact(line.lineTotalCents, sign)),
                    decimal(Math.multiplyExact(line.costTotalCents, costSign)),
                    profit,
                    decimal(Math.multiplyExact(document.totalCents, sign)),
                    document.note,
                    document.originalDocumentId?.toString().orEmpty(),
                    document.originalType?.let(::label).orEmpty(),
                    line.quantityDelta.toString(),
                    decimal(line.stockCostDeltaCents),
                    decimal(line.valuationDifferenceCents),
                    line.costSource,
                )
            }
        }
    }

    private fun decimal(cents: Long): String = java.math.BigDecimal.valueOf(cents, 2).toPlainString()

    private fun label(type: DocumentType): String = when(type) {
        DocumentType.INBOUND -> "入货"
        DocumentType.OUTBOUND -> "出货"
        DocumentType.OPENING -> "期初库存"
        DocumentType.SALES_RETURN -> "销售退货"
        DocumentType.PURCHASE_RETURN -> "进货退货"
        DocumentType.STOCK_GAIN -> "盘盈"
        DocumentType.STOCK_LOSS -> "盘亏"
        DocumentType.WRITE_OFF -> "报损"
        DocumentType.REVERSAL -> "冲正"
    }

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }

    private fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
}
