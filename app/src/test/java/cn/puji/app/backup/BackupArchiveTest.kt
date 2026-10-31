package cn.puji.app.backup

import org.junit.Assert.*
import org.junit.Test

class BackupArchiveTest {
    private val schema = BackupArchive.TABLES.associateWith { table ->
        buildList {
            add(BackupColumn("id", "INTEGER", false, 1))
            add(BackupColumn("uuid", "TEXT", true, 0))
            if (table != "products") add(BackupColumn(if (table == "documents") "type" else "product_id", if (table == "documents") "TEXT" else "INTEGER", true, 0))
            if (table == "document_lines" || table == "stock_movements") add(BackupColumn("document_id", "INTEGER", true, 0))
            if (table == "stock_movements") add(BackupColumn("type", "TEXT", true, 0))
        }
    }
    private fun rows() = BackupArchive.TABLES.associateWith { table ->
        listOf(buildMap<String, Any?> {
            put("id", 1L); put("uuid", "00000000-0000-4000-8000-000000000001")
            if (table == "documents" || table == "stock_movements") put("type", "OPENING")
            if (table == "document_lines" || table == "stock_movements") { put("product_id", 1L); put("document_id", 1L) }
        })
    }

    @Test fun roundTripPreservesAllColumnsAndMetadata() {
        val content = BackupArchive.encode(3, 1234, schema, rows())
        val archive = BackupArchive.decode(content, 3, schema)
        assertEquals(1234L, archive.info.createdAt)
        assertEquals(1, archive.info.productCount)
        assertEquals(rows(), archive.tables)
    }

    @Test fun schemaOrderDoesNotDependOnUpgradeColumnInsertionOrder() {
        val reordered = schema.mapValues { (_, columns) -> columns.reversed() }
        val original = BackupArchive.encode(3, 1234, schema, rows())
        val fromMigratedLayout = BackupArchive.encode(3, 1234, reordered, rows())
        assertEquals(original, fromMigratedLayout)
        assertEquals(rows(), BackupArchive.decode(original, 3, reordered).tables)
        assertEquals(rows(), BackupArchive.decode(fromMigratedLayout, 3, schema).tables)

        // Also accept already-exported archives whose column arrays were not sorted.
        val root = (BackupJson.parse(original) as Map<*, *>).entries.associate { it.key as String to it.value }.toMutableMap()
        val payload = (root.getValue("payload") as Map<*, *>).entries.associate { it.key as String to it.value }.toMutableMap()
        val exportedSchema = (payload.getValue("schema") as Map<*, *>).entries.associate { (table, columns) -> table as String to (columns as List<*>).reversed() }
        payload["schema"] = exportedSchema
        root["payload"] = payload
        root["sha256"] = java.security.MessageDigest.getInstance("SHA-256").digest(BackupJson.stringify(payload).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals(rows(), BackupArchive.decode(BackupJson.stringify(root), 3, schema).tables)
    }

    @Test fun rejectsCorruptionCsvAndUnsupportedSchema() {
        val content = BackupArchive.encode(3, 1234, schema, rows())
        for (bad in listOf(content.replace("OPENING", "OUTBOUND"), "名称,条码\n可乐,1", content + "x")) {
            assertTrue(runCatching { BackupArchive.decode(bad, 3, schema) }.isFailure)
        }
        assertTrue(runCatching { BackupArchive.decode(content, 4, schema) }.isFailure)
    }

    @Test fun rejectsDuplicatePrimaryKeyUuidMissingParentAndForeignColumns() {
        val duplicate = rows().toMutableMap().apply { put("products", getValue("products") + getValue("products")) }
        val missing = rows().toMutableMap().apply { put("products", emptyList()) }
        val foreign = rows().toMutableMap().apply { put("products", listOf(getValue("products").single() + ("evil" to 1L))) }
        val invalidUuid = rows().toMutableMap().apply { put("products", listOf(getValue("products").single() + ("uuid" to "not-a-uuid"))) }
        val duplicateUuid = rows().toMutableMap().apply { put("products", getValue("products") + (getValue("products").single() + ("id" to 2L))) }
        for (bad in listOf(duplicate, missing, foreign, invalidUuid, duplicateUuid)) {
            assertTrue(runCatching { BackupArchive.decode(BackupArchive.encode(3, 1234, schema, bad), 3, schema) }.isFailure)
        }
    }

    @Test fun strictJsonRejectsDuplicateKeysFractionalNumbersAndDeepNesting() {
        for (bad in listOf("{\"a\":1,\"a\":2}", "{\"a\":1.1}", "[".repeat(50) + "0" + "]".repeat(50))) {
            assertTrue(runCatching { BackupJson.parse(bad) }.isFailure)
        }
    }

    @Test fun includesOnlySupportedBooleanSettingsInDigest() {
        val content = BackupArchive.encode(3, 1234, schema, rows(), mapOf("scanner_feedback" to false))
        assertEquals(mapOf("scanner_feedback" to false), BackupArchive.decode(content, 3, schema).settings)
        val foreign = BackupArchive.encode(3, 1234, schema, rows(), mapOf("unknown_setting" to true))
        assertTrue(runCatching { BackupArchive.decode(foreign, 3, schema) }.isFailure)
    }

    @Test fun rejectsNegativeInventoryEvenWithValidDigest() {
        val stockSchema = schema.toMutableMap().apply { put("products", getValue("products") + BackupColumn("stock_quantity", "INTEGER", true, 0)) }
        val negative = rows().toMutableMap().apply { put("products", listOf(getValue("products").single() + ("stock_quantity" to -1L))) }
        assertTrue(runCatching { BackupArchive.decode(BackupArchive.encode(3, 1234, stockSchema, negative), 3, stockSchema) }.isFailure)
    }

    @Test fun openingCostIsNotMistakenForPurchaseCash() {
        val cashSchema = schema.toMutableMap().apply {
            put("documents", getValue("documents") + BackupColumn("total_cents", "INTEGER", true, 0))
            put("document_lines", getValue("document_lines") + listOf(BackupColumn("quantity", "INTEGER", true, 0), BackupColumn("unit_price_cents", "INTEGER", true, 0), BackupColumn("line_total_cents", "INTEGER", true, 0)))
        }
        val opening = rows().toMutableMap().apply {
            put("documents", listOf(getValue("documents").single() + ("total_cents" to 0L)))
            put("document_lines", listOf(getValue("document_lines").single() + mapOf("quantity" to 3L, "unit_price_cents" to 200L, "line_total_cents" to 600L)))
        }
        val decoded = BackupArchive.decode(BackupArchive.encode(3, 1234, cashSchema, opening), 3, cashSchema)
        assertEquals(0L, decoded.tables.getValue("documents").single()["total_cents"])
    }
}
