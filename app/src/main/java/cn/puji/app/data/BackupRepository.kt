package cn.puji.app.data

import android.content.ContentValues
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import cn.puji.app.backup.BackupArchive
import cn.puji.app.backup.BackupColumn
import cn.puji.app.backup.BackupInfo
import cn.puji.app.backup.DecodedBackup

/** Call from a background thread. The caller saves a rollback archive before restoring. */
class BackupRepository(private val database: PujiDatabase, private val settings: SharedPreferences? = null) {
    fun exportJson(): String {
        val db = database.readableDatabase
        db.beginTransactionNonExclusive()
        try {
            val schema = schema(db)
            val tables = BackupArchive.TABLES.associateWith { table ->
                db.query(table, schema.getValue(table).map { it.name }.toTypedArray(), null, null, null, null, "id ASC").use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(schema.getValue(table).mapIndexed { index, column ->
                                column.name to when (cursor.getType(index)) {
                                    Cursor.FIELD_TYPE_NULL -> null
                                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                                    Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                                    else -> throw IllegalArgumentException("数据库含非法数字或不支持的数据类型")
                                }
                            }.toMap())
                        }
                    }
                }
            }
            val preferences = settings?.let { mapOf("scanner_feedback" to it.getBoolean("scanner_feedback", true)) } ?: emptyMap()
            val content = BackupArchive.encode(db.version, System.currentTimeMillis(), schema, tables, preferences)
            // Refuse to label a malformed database as a usable backup.
            BackupArchive.decode(content, db.version, schema)
            db.setTransactionSuccessful()
            return content
        } finally {
            db.endTransaction()
        }
    }

    fun inspect(content: String): BackupInfo {
        val db = database.readableDatabase
        val archive = BackupArchive.decode(content, db.version, schema(db))
        validateInTemporaryDatabase(db, archive)
        return archive.info
    }

    fun restoreJson(content: String): Result<Unit> = runCatching {
        val db = database.writableDatabase
        val archive = BackupArchive.decode(content, db.version, schema(db))
        // All validation and constraint checks happen before touching the current business data.
        validateInTemporaryDatabase(db, archive)
        val preferences = settings
        val restoreFeedback = archive.settings["scanner_feedback"]
        val oldContains = preferences?.contains("scanner_feedback") ?: false
        val oldFeedback = preferences?.getBoolean("scanner_feedback", true) ?: true
        try {
            replace(db, archive) {
                if (preferences != null && restoreFeedback != null) {
                    require(preferences.edit().putBoolean("scanner_feedback", restoreFeedback).commit()) { "恢复扫码设置失败" }
                }
            }
        } catch (error: Exception) {
            if (preferences != null && restoreFeedback != null) {
                val editor = preferences.edit()
                if (oldContains) editor.putBoolean("scanner_feedback", oldFeedback) else editor.remove("scanner_feedback")
                editor.commit()
            }
            throw error
        }
    }

    private fun schema(db: SQLiteDatabase): Map<String, List<BackupColumn>> = BackupArchive.TABLES.associateWith { table ->
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    require(name.matches(Regex("[a-z_][a-z0-9_]*"))) { "当前数据库字段格式不受支持" }
                    add(BackupColumn(name, cursor.getString(cursor.getColumnIndexOrThrow("type")).uppercase(), cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1, cursor.getInt(cursor.getColumnIndexOrThrow("pk"))))
                }
                require(isNotEmpty()) { "业务数据表缺失" }
            }
        }
    }

    private fun validateInTemporaryDatabase(current: SQLiteDatabase, archive: DecodedBackup) {
        SQLiteDatabase.create(null).use { temporary ->
            temporary.setForeignKeyConstraintsEnabled(true)
            BackupArchive.TABLES.forEach { table ->
                val sql = current.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use {
                    require(it.moveToFirst()) { "当前数据库结构不完整" }; it.getString(0)
                }
                // SQL comes only from this installed database, never from the archive.
                temporary.execSQL(sql)
            }
            // Reproduce local UNIQUE and CHECK constraints as part of the preflight.
            BackupArchive.TABLES.forEach { table ->
                current.rawQuery("SELECT sql FROM sqlite_master WHERE type='index' AND tbl_name=? AND sql IS NOT NULL", arrayOf(table)).use {
                    while (it.moveToNext()) temporary.execSQL(it.getString(0))
                }
            }
            replace(temporary, archive)
        }
    }

    private fun replace(db: SQLiteDatabase, archive: DecodedBackup, beforeCommit: () -> Unit = {}) {
        val allowed = schema(db)
        db.beginTransaction()
        try {
            // Self-references and references to later rows remain checked at transaction completion.
            db.execSQL("PRAGMA defer_foreign_keys=ON")
            BackupArchive.TABLES.asReversed().forEach { table -> db.delete(table, null, null) }
            BackupArchive.TABLES.forEach { table ->
                db.delete("sqlite_sequence", "name=?", arrayOf(table))
                archive.tables.getValue(table).forEach { row ->
                    require(row.keys == allowed.getValue(table).map { it.name }.toSet()) { "恢复字段发生变化" }
                    val values = ContentValues(row.size)
                    allowed.getValue(table).forEach { column ->
                        when (val value = row[column.name]) {
                            null -> values.putNull(column.name)
                            is String -> values.put(column.name, value)
                            is Long -> values.put(column.name, value)
                            else -> throw IllegalArgumentException("恢复数据类型无效")
                        }
                    }
                    db.insertOrThrow(table, null, values)
                }
            }
            db.rawQuery("PRAGMA foreign_key_check", null).use { require(!it.moveToFirst()) { "恢复文件包含失效关联" } }
            db.rawQuery("PRAGMA quick_check", null).use { require(it.moveToFirst() && it.getString(0) == "ok") { "恢复数据库完整性检查失败" } }
            beforeCommit()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
