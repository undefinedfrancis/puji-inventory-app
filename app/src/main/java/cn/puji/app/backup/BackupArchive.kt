package cn.puji.app.backup

import java.security.MessageDigest
import java.util.UUID

data class BackupInfo(val createdAt: Long, val version: Int, val productCount: Int, val documentCount: Int)
internal data class BackupColumn(val name: String, val type: String, val notNull: Boolean, val primaryKey: Int)
internal data class DecodedBackup(val info: BackupInfo, val tables: Map<String, List<Map<String, Any?>>>, val settings: Map<String, Boolean>)

/** The digest covers the canonical payload, including metadata, schema and every business column. */
internal object BackupArchive {
    const val MAX_BYTES = 32 * 1024 * 1024
    private const val MAX_ROWS = 200_000
    private const val FORMAT_VERSION = 1
    val TABLES = listOf("products", "documents", "document_lines", "stock_movements")
    private val documentTypes = setOf("INBOUND", "OUTBOUND", "OPENING", "SALES_RETURN", "PURCHASE_RETURN", "STOCK_GAIN", "STOCK_LOSS", "WRITE_OFF", "REVERSAL")
    private val signedColumns = setOf("quantity_delta", "stock_cost_delta_cents", "valuation_difference_cents")
    private val intColumns = setOf("stock_quantity", "low_stock_threshold", "quantity", "quantity_delta", "stock_after", "expected_quantity", "actual_quantity")

    fun encode(version: Int, createdAt: Long, schema: Map<String, List<BackupColumn>>, tables: Map<String, List<Map<String, Any?>>>, settings: Map<String, Boolean> = emptyMap()): String {
        val payload = linkedMapOf<String, Any?>(
            "schemaVersion" to version, "createdAt" to createdAt,
            "schema" to schemaJson(schema), "tables" to tables,
            "settings" to settings,
            "rowCounts" to tables.mapValues { it.value.size },
            "metadata" to mapOf("application" to "cn.puji.app", "currency" to "CNY", "moneyUnit" to "cent", "encoding" to "UTF-8"),
        )
        return BackupJson.stringify(mapOf("format" to "puji-offline-backup", "version" to FORMAT_VERSION, "sha256" to digest(payload), "payload" to payload)).also {
            require(it.length <= MAX_BYTES && it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "备份超过 32 MB，请联系支持后处理" }
        }
    }

    fun decode(content: String, expectedVersion: Int, schema: Map<String, List<BackupColumn>>): DecodedBackup {
        require(content.length <= MAX_BYTES && content.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "备份不能超过 32 MB" }
        val root = objectValue(BackupJson.parse(content.removePrefix("\uFEFF")))
        require(root.keys == setOf("format", "version", "sha256", "payload")) { "备份归档结构不支持" }
        require(root["format"] == "puji-offline-backup" && root["version"] == FORMAT_VERSION.toLong()) { "不支持此备份格式版本" }
        val payload = objectValue(root["payload"])
        require(payload.keys == setOf("schemaVersion", "createdAt", "schema", "tables", "rowCounts", "metadata", "settings")) { "备份内容结构不支持" }
        val sha = root["sha256"] as? String ?: throw IllegalArgumentException("备份缺少校验信息")
        require(sha.matches(Regex("[0-9a-f]{64}")) && MessageDigest.isEqual(sha.toByteArray(), digest(payload).toByteArray())) { "备份校验失败，文件可能已损坏" }
        require(payload["schemaVersion"] == expectedVersion.toLong()) { "备份数据版本与当前应用不一致" }
        // Migration can append columns in a different order. Compare named fields against
        // local PRAGMA output; archive order never changes the accepted column allowlist.
        require(BackupJson.stringify(normalizedSchemaJson(payload["schema"])) == BackupJson.stringify(schemaJson(schema))) { "备份字段结构与当前应用不一致" }
        require(objectValue(payload["metadata"]) == mapOf("application" to "cn.puji.app", "currency" to "CNY", "moneyUnit" to "cent", "encoding" to "UTF-8")) { "备份元信息不支持" }
        val createdAt = payload["createdAt"] as? Long ?: throw IllegalArgumentException("备份日期无效")
        require(createdAt >= 0) { "备份日期无效" }
        val rawSettings = objectValue(payload["settings"])
        require(rawSettings.keys.all { it == "scanner_feedback" } && rawSettings.values.all { it is Boolean }) { "备份设置字段不支持" }
        val settings = rawSettings.mapValues { it.value as Boolean }
        val rawTables = objectValue(payload["tables"])
        val counts = objectValue(payload["rowCounts"])
        require(rawTables.keys == TABLES.toSet() && counts.keys == TABLES.toSet()) { "备份业务表不完整" }
        var totalRows = 0
        val tables = TABLES.associateWith { table ->
            val rows = rawTables[table] as? List<*> ?: throw IllegalArgumentException("备份 $table 表无效")
            totalRows = Math.addExact(totalRows, rows.size)
            require(totalRows <= MAX_ROWS) { "备份行数超过 200000 行" }
            require(counts[table] == rows.size.toLong()) { "备份行数校验失败" }
            rows.map { objectValue(it) }
        }
        validate(schema, tables)
        return DecodedBackup(BackupInfo(createdAt, expectedVersion, tables.getValue("products").size, tables.getValue("documents").size), tables, settings)
    }

    private fun schemaJson(schema: Map<String, List<BackupColumn>>) = schema.mapValues { (_, columns) ->
        columns.sortedBy { it.name }.map { mapOf("name" to it.name, "type" to it.type, "notNull" to it.notNull, "primaryKey" to it.primaryKey) }
    }

    private fun normalizedSchemaJson(value: Any?): Map<String, List<Map<String, Any?>>> = objectValue(value).mapValues { (_, valueColumns) ->
        val columns = (valueColumns as? List<*>)?.map { objectValue(it) }
            ?: throw IllegalArgumentException("备份字段结构无效")
        columns.forEach { column ->
            require(column.keys == setOf("name", "type", "notNull", "primaryKey") && column["name"] is String && column["type"] is String && column["notNull"] is Boolean && column["primaryKey"] is Long) { "备份字段定义无效" }
        }
        require(columns.map { it["name"] }.distinct().size == columns.size) { "备份字段名称重复" }
        columns.sortedBy { it["name"] as String }
    }
    private fun digest(payload: Any?): String = MessageDigest.getInstance("SHA-256")
        .digest(BackupJson.stringify(payload).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun validate(schema: Map<String, List<BackupColumn>>, tables: Map<String, List<Map<String, Any?>>>) {
        tables.forEach { (table, rows) ->
            val columns = schema.getValue(table)
            val ids = mutableSetOf<Long>()
            val uuids = mutableSetOf<String>()
            val requests = mutableSetOf<String>()
            val barcodes = mutableSetOf<String>()
            rows.forEach { row ->
                require(row.keys == columns.map { it.name }.toSet()) { "$table 表包含缺失或未知字段" }
                columns.forEach { column ->
                    val value = row[column.name]
                    if (value == null) require(!column.notNull && column.primaryKey == 0) { "$table.${column.name} 不能为空" }
                    else when (column.type.uppercase()) {
                        "INTEGER" -> {
                            require(value is Long) { "$table.${column.name} 必须为整数" }
                            if (column.name in intColumns) require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "库存或数量超出范围" }
                            if (column.name !in signedColumns) require(value >= 0) { "$table.${column.name} 不能为负数" }
                            else require(value != Long.MIN_VALUE) { "库存变动数值超出范围" }
                        }
                        "TEXT" -> require(value is String && value.length <= 100_000 && '\u0000' !in value) { "$table.${column.name} 文本无效或过长" }
                        else -> throw IllegalArgumentException("当前备份不支持此字段类型")
                    }
                }
                val id = row["id"] as? Long ?: throw IllegalArgumentException("记录缺少主键")
                require(id > 0 && id < Long.MAX_VALUE && ids.add(id)) { "$table 表主键重复或无效" }
                if (row.containsKey("uuid")) {
                    val uuid = row["uuid"] as? String ?: ""
                    require(runCatching { UUID.fromString(uuid).toString().equals(uuid, true) }.getOrDefault(false) && uuids.add(uuid.lowercase())) { "$table 表 UUID 重复或无效" }
                }
                if (row.containsKey("version")) require((row["version"] as Long) > 0) { "记录版本无效" }
                if (row.containsKey("request_id")) {
                    val request = row["request_id"] as String
                    require(request.isNotBlank() && request.length <= 2000 && requests.add(request)) { "业务请求 ID 重复或无效" }
                }
                if (table == "products") {
                    if (row.containsKey("active")) require(row["active"] == 0L || row["active"] == 1L) { "商品状态无效" }
                    (row["name"] as? String)?.let { require(it.isNotBlank()) { "商品名称为空" } }
                    (row["barcode"] as? String)?.takeIf { it.isNotEmpty() }?.let { require(barcodes.add(it)) { "商品条码重复" } }
                }
                if (table == "documents" || table == "stock_movements") require(row["type"] in documentTypes) { "单据类型不支持" }
                row["original_type"]?.let { require(it in documentTypes) { "原单类型不支持" } }
                if (table == "document_lines" && row.containsKey("quantity")) {
                    val quantity = row["quantity"] as Long
                    require(quantity > 0) { "明细数量必须大于零" }
                    val unit = row["unit_price_cents"] as? Long
                    if (unit != null) require(Math.multiplyExact(unit, quantity) == row["line_total_cents"]) { "明细金额与数量不一致" }
                }
            }
        }
        val products = tables.getValue("products").associateBy { it["id"] as Long }
        val documents = tables.getValue("documents").associateBy { it["id"] as Long }
        val lines = tables.getValue("document_lines").associateBy { it["id"] as Long }
        tables.getValue("documents").forEach { row ->
            require((row["expected_quantity"] == null) == (row["actual_quantity"] == null)) { "盘点数量必须成对保存" }
            (row["original_document_id"] as? Long)?.let { original ->
                require(original != row["id"] && documents.containsKey(original)) { "原单关联不存在或引用自身" }
                row["original_type"]?.let { require(it == documents.getValue(original)["type"]) { "原单类型与关联单据不一致" } }
            }
        }
        for (table in listOf("document_lines", "stock_movements")) tables.getValue(table).forEach { row ->
            require(products.containsKey(row["product_id"]) && documents.containsKey(row["document_id"])) { "商品或单据关联缺失" }
            if (table == "stock_movements") require(row["type"] == documents.getValue(row["document_id"] as Long)["type"]) { "库存流水类型与单据不一致" }
            row["original_line_id"]?.let { original ->
                val parent = lines[original] ?: throw IllegalArgumentException("原单明细关联缺失")
                require(original != row["id"] && parent["product_id"] == row["product_id"]) { "原单明细商品不匹配" }
                require(documents.getValue(row["document_id"] as Long)["original_document_id"] == parent["document_id"]) { "明细与原单关联不一致" }
            }
        }
        // Detect cycles without recursive calls, even when IDs were reordered in the archive.
        acyclic(documents, "original_document_id")
        acyclic(lines, "original_line_id")
        val totals = mutableMapOf<Long, Long>()
        tables.getValue("document_lines").forEach { line ->
            val amount = line["line_total_cents"] as? Long ?: return@forEach
            val documentId = line["document_id"] as Long
            totals[documentId] = Math.addExact(totals[documentId] ?: 0L, amount)
        }
        val cashTypes = setOf("INBOUND", "OUTBOUND", "SALES_RETURN", "PURCHASE_RETURN")
        documents.forEach { (id, doc) ->
            if (doc.containsKey("total_cents")) {
                val affectsCash = doc["type"] in cashTypes || (doc["type"] == "REVERSAL" && doc["original_type"] in cashTypes)
                require(doc["total_cents"] == if (affectsCash) (totals[id] ?: 0L) else 0L) { "单据总额与业务明细不一致" }
            }
        }
    }

    private fun acyclic(rows: Map<Long, Map<String, Any?>>, column: String) {
        val done = mutableSetOf<Long>()
        rows.keys.forEach { start ->
            val path = mutableSetOf<Long>()
            var current: Long? = start
            while (current != null && current !in done) {
                require(path.add(current)) { "备份包含循环关联" }
                current = rows[current]?.get(column) as? Long
            }
            done.addAll(path)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun objectValue(value: Any?): Map<String, Any?> {
        require(value is Map<*, *> && value.keys.all { it is String }) { "备份 JSON 对象无效" }
        return value as Map<String, Any?>
    }
}
