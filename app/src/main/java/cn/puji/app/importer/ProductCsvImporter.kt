package cn.puji.app.importer

import cn.puji.app.data.ProductRepository
import cn.puji.app.domain.Product
import java.math.BigDecimal

data class CsvProductRow(val lineNumber: Int, val product: Product)
data class CsvImportError(val row: Int, val message: String)
data class CsvImportPreview(val rows: List<CsvProductRow>, val errors: List<CsvImportError>)
data class ImportResult(val created: Int, val updated: Int)

/** CSV is a product-entry format. It never represents a restorable business ledger. */
object ProductCsvImporter {
    private const val MAX_BYTES = 5 * 1024 * 1024
    private const val MAX_ROWS = 10_000
    private val columns = listOf("名称", "条码", "分类", "进货价", "售价", "库存", "预警值", "备注")
    private val aliases = mapOf("商品名称" to "名称", "条形码" to "条码", "进价" to "进货价", "销售价" to "售价", "期初库存" to "库存", "初始库存" to "库存")

    fun template(): String = "\uFEFF${columns.joinToString(",")}\r\n"

    fun preview(csv: String): CsvImportPreview {
        val errors = mutableListOf<CsvImportError>()
        val rows = mutableListOf<CsvProductRow>()
        if (csv.length > MAX_BYTES || csv.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
            return CsvImportPreview(emptyList(), listOf(CsvImportError(1, "商品表不能超过 5 MB")))
        }
        val parsed = runCatching { parseCsv(csv.removePrefix("\uFEFF")) }.getOrElse {
            return CsvImportPreview(emptyList(), listOf(CsvImportError(1, it.message ?: "CSV 格式错误")))
        }
        if (parsed.isEmpty()) return CsvImportPreview(emptyList(), listOf(CsvImportError(1, "请选择商品 CSV 文件")))
        val header = parsed.first().second.map { aliases[it.trim()] ?: it.trim() }
        if (header.any { it in setOf("单号", "类型", "单据总额", "数量", "小计") }) {
            return CsvImportPreview(emptyList(), listOf(CsvImportError(1, "流水 CSV 不能作为商品表导入")))
        }
        if (header.distinct().size != header.size || !header.containsAll(columns) || header.any { it !in columns && it != "更新时间" }) {
            return CsvImportPreview(emptyList(), listOf(CsvImportError(1, "表头不匹配，请使用商品导入模板或库存导出表")))
        }
        if (parsed.size - 1 > MAX_ROWS) return CsvImportPreview(emptyList(), listOf(CsvImportError(1, "每次最多导入 10000 行")))
        val barcodeLines = mutableMapOf<String, MutableList<Int>>()
        parsed.drop(1).forEach { (line, cells) ->
            if (cells.all { it.isBlank() }) return@forEach
            if (cells.size != header.size) {
                errors += CsvImportError(line, "列数与表头不一致")
                return@forEach
            }
            val values = header.zip(cells).toMap()
            val barcode = values.getValue("条码").trim()
            if (barcode.isNotEmpty()) barcodeLines.getOrPut(barcode) { mutableListOf() }.add(line)
            runCatching {
                val product = Product(
                    name = values.getValue("名称").trim(), barcode = barcode,
                    category = values.getValue("分类").trim().ifEmpty { "未分类" },
                    costPriceCents = price(values.getValue("进货价")),
                    salePriceCents = price(values.getValue("售价")),
                    stockQuantity = quantity(values.getValue("库存"), "库存"),
                    lowStockThreshold = quantity(values.getValue("预警值"), "预警值"),
                    note = values.getValue("备注"),
                )
                validateProduct(product)
                rows += CsvProductRow(line, product)
            }.onFailure { errors += CsvImportError(line, it.message ?: "商品资料格式错误") }
        }
        barcodeLines.filterValues { it.size > 1 }.forEach { (barcode, lines) ->
            lines.forEach { errors += CsvImportError(it, "条码 $barcode 在文件内重复") }
        }
        return CsvImportPreview(rows.toList(), errors.sortedBy { it.row })
    }

    fun importRows(repository: ProductRepository, preview: CsvImportPreview): Result<ImportResult> = runCatching {
        require(preview.errors.isEmpty()) { "请先修正预览中的错误" }
        require(preview.rows.isNotEmpty()) { "没有可导入的商品" }
        require(preview.rows.size <= MAX_ROWS) { "每次最多导入 10000 行" }
        val products = preview.rows.map { it.product.also(::validateProduct) }
        require(products.all { it.id == 0L }) { "导入商品必须通过条码匹配" }
        val nonemptyBarcodes = products.map { it.barcode.trim() }.filter { it.isNotEmpty() }
        require(nonemptyBarcodes.distinct().size == nonemptyBarcodes.size) { "文件中存在重复条码" }
        val existing = repository.list(includeInactive = true).map { it.barcode }.filter { it.isNotBlank() }.toSet()
        val updated = products.count { it.barcode.isNotBlank() && it.barcode in existing }
        repository.importProducts(products).getOrThrow()
        ImportResult(created = products.size - updated, updated = updated)
    }

    private fun price(raw: String): Long {
        val value = raw.trim()
        require(value.length <= 24 && value.matches(Regex("[0-9]+(?:\\.[0-9]{1,2})?"))) { "价格须为范围内的非负金额，最多两位小数" }
        return try { BigDecimal(value).movePointRight(2).longValueExact() }
        catch (_: ArithmeticException) { throw IllegalArgumentException("价格超出范围") }
    }

    private fun quantity(raw: String, name: String): Int {
        val value = raw.trim()
        require(value.length <= 10 && value.matches(Regex("[0-9]+"))) { "$name 须为范围内的非负整数" }
        return value.toIntOrNull() ?: throw IllegalArgumentException("$name 超出范围")
    }

    private fun validateProduct(product: Product) {
        require(product.name.isNotBlank() && product.name.length <= 500) { "商品名称不能为空且不能超过 500 字" }
        require(product.barcode.length <= 200 && product.barcode.none { it.isISOControl() }) { "条码格式错误或过长" }
        require(product.category.length <= 200 && product.note.length <= 10_000) { "分类或备注过长" }
        require(product.costPriceCents >= 0 && product.salePriceCents >= 0 && product.stockQuantity >= 0 && product.lowStockThreshold >= 0) { "金额和数量不能为负数" }
        try { Math.multiplyExact(product.costPriceCents, product.stockQuantity.toLong()) }
        catch (_: ArithmeticException) { throw IllegalArgumentException("期初库存金额超出范围") }
    }

    /** RFC 4180 quotes, including escaped quotes and physical newlines inside a cell. */
    private fun parseCsv(text: String): List<Pair<Int, List<String>>> {
        val result = mutableListOf<Pair<Int, List<String>>>()
        var cells = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var closed = false
        var line = 1
        var rowLine = 1
        var index = 0
        fun finishCell() { cells.add(cell.toString()); cell.setLength(0); closed = false }
        fun finishRow() { finishCell(); result.add(rowLine to cells.toList()); cells = mutableListOf() }
        while (index < text.length) {
            val c = text[index]
            if (quoted) {
                if (c == '"') {
                    if (index + 1 < text.length && text[index + 1] == '"') { cell.append('"'); index++ }
                    else { quoted = false; closed = true }
                } else {
                    cell.append(c)
                    if (c == '\n' || (c == '\r' && text.getOrNull(index + 1) != '\n')) line++
                }
            } else when (c) {
                '"' -> { require(cell.isEmpty() && !closed) { "第 $line 行引号位置错误" }; quoted = true }
                ',' -> finishCell()
                '\r', '\n' -> {
                    finishRow()
                    if (c == '\r' && text.getOrNull(index + 1) == '\n') index++
                    line++; rowLine = line
                    require(result.size <= MAX_ROWS + 1) { "每次最多导入 10000 行" }
                }
                else -> { require(!closed) { "第 $line 行闭引号后存在多余内容" }; cell.append(c) }
            }
            index++
        }
        require(!quoted) { "第 $rowLine 行引号未闭合" }
        if (cell.isNotEmpty() || cells.isNotEmpty() || closed) finishRow()
        return result
    }
}
