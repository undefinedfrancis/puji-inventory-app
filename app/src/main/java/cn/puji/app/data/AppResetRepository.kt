package cn.puji.app.data

class AppResetRepository(private val database: PujiDatabase) {
    fun clearAll(): Result<Unit> = runCatching {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.delete("stock_movements", null, null)
            db.delete("document_lines", null, null)
            db.delete("documents", null, null)
            db.delete("products", null, null)
            db.delete("sqlite_sequence", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
